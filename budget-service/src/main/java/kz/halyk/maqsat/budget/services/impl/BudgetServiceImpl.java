package kz.halyk.maqsat.budget.services.impl;

import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import kz.halyk.maqsat.budget.dto.req.CreatePlanRequest;
import kz.halyk.maqsat.budget.dto.req.ParsedPlanPayload;
import kz.halyk.maqsat.budget.dto.res.ActivePlanView;
import kz.halyk.maqsat.budget.dto.res.DashboardResponse;
import kz.halyk.maqsat.budget.entities.BudgetCategory;
import kz.halyk.maqsat.budget.entities.BudgetPlan;
import kz.halyk.maqsat.budget.entities.enums.OwnerType;
import kz.halyk.maqsat.budget.exceptions.PlanNotFoundException;
import kz.halyk.maqsat.budget.mappers.BudgetMapper;
import kz.halyk.maqsat.budget.repositories.BudgetPlanRepository;
import kz.halyk.maqsat.budget.services.BudgetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetServiceImpl implements BudgetService {

    private final BudgetPlanRepository planRepository;
    private final BudgetMapper budgetMapper;
    private final MeterRegistry meterRegistry;

    @Override
    @Transactional
    public BudgetPlan createPlan(String currentUserId, CreatePlanRequest request) {
        BudgetPlan plan = new BudgetPlan();
        plan.setOwnerType(request.ownerType() != null ? request.ownerType() : OwnerType.USER);
        plan.setOwnerId(request.ownerId() != null ? request.ownerId() : currentUserId);
        plan.setPeriodStart(request.periodStart());
        plan.setPeriodEnd(request.periodEnd());
        plan.setCreatedAt(Instant.now());

        request.categories().forEach(c -> {
            BudgetCategory category = new BudgetCategory();
            category.setName(c.name());
            category.setType(c.type());
            category.setLimitAmount(c.limitAmount());
            category.setSpentAmount(BigDecimal.ZERO);
            plan.addCategory(category);
        });

        return planRepository.save(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse dashboard(String currentUserId) {
        BudgetPlan plan = activePlan(currentUserId, OwnerType.USER, LocalDate.now())
                .orElseThrow(() -> new PlanNotFoundException("No active budget plan for the current user"));

        BigDecimal totalLimit = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;
        var views = new java.util.ArrayList<DashboardResponse.CategoryView>();
        for (BudgetCategory c : plan.getCategories()) {
            views.add(budgetMapper.toCategoryView(c));
            totalLimit = totalLimit.add(c.getLimitAmount());
            totalSpent = totalSpent.add(c.getSpentAmount());
        }
        return new DashboardResponse(plan.getId(), plan.getPeriodStart(), plan.getPeriodEnd(), totalLimit, totalSpent, views);
    }

    @Override
    @Transactional
    public void trackSpending(String ownerId, String categoryName, BigDecimal amount, LocalDate date) {
        Optional<BudgetPlan> plan = activePlan(ownerId, OwnerType.USER, date);
        if (plan.isEmpty()) {
            log.debug("No active plan for owner {} on {}, skipping", ownerId, date);
            return;
        }
        plan.get().getCategories().stream()
                .filter(c -> c.getName().equalsIgnoreCase(categoryName))
                .findFirst()
                .ifPresentOrElse(
                        c -> {
                            c.setSpentAmount(c.getSpentAmount().add(amount));
                            meterRegistry.counter("maqsat.budget.tracked", "category", categoryName).increment();
                            log.info("Tracked {} to category '{}' (spent now {})", amount, categoryName, c.getSpentAmount());
                        },
                        () -> log.debug("No category '{}' in plan {}", categoryName, plan.get().getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public ActivePlanView getActivePlan(String userId) {
        Optional<BudgetPlan> planOpt = activePlan(userId, OwnerType.USER, LocalDate.now());
        if (planOpt.isEmpty()) return null;
        BudgetPlan plan = planOpt.get();
        String period = DateTimeFormatter.ofPattern("yyyy-MM").format(plan.getPeriodStart());
        List<ActivePlanView.CategoryLimitView> cats = plan.getCategories().stream()
                .map(budgetMapper::toCategoryLimitView).toList();
        return new ActivePlanView(plan.getId(), period, cats);
    }

    @Override
    @Transactional
    public BudgetPlan replan(String userId, ParsedPlanPayload payload) {
        OwnerType type = payload.ownerType() != null ? payload.ownerType() : OwnerType.USER;
        String ownerId = payload.ownerId() != null ? payload.ownerId() : userId;

        BudgetPlan previous = planRepository
                .findFirstByOwnerIdAndOwnerTypeAndSupersededByIsNullOrderByVersionDesc(ownerId, type)
                .orElse(null);
        int nextVersion = previous == null ? 1 : previous.getVersion() + 1;

        YearMonth ym = YearMonth.parse(payload.period());
        BudgetPlan next = new BudgetPlan();
        next.setOwnerId(ownerId);
        next.setOwnerType(type);
        next.setPeriodStart(ym.atDay(1));
        next.setPeriodEnd(ym.atEndOfMonth());
        next.setCreatedAt(Instant.now());
        next.setVersion(nextVersion);
        next.setCreatedByAi(payload.createdByAi());
        payload.categories().forEach(pc -> {
            BudgetCategory c = new BudgetCategory();
            c.setName(pc.name());
            c.setType(pc.type());
            c.setLimitAmount(pc.limitAmount());
            c.setSpentAmount(BigDecimal.ZERO);
            next.addCategory(c);
        });
        planRepository.save(next);

        if (previous != null) {
            previous.setSupersededBy(next.getId());
            planRepository.save(previous);
        }
        log.info("Replanned user={} version {} → {} (createdByAi={})",
                ownerId, nextVersion - 1, nextVersion, payload.createdByAi());
        return next;
    }

    private Optional<BudgetPlan> activePlan(String ownerId, OwnerType ownerType, LocalDate date) {
        return planRepository
                .findFirstByOwnerIdAndOwnerTypeAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqualAndSupersededByIsNullOrderByPeriodStartDesc(
                        ownerId, ownerType, date, date);
    }
}
