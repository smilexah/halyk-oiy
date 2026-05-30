package kz.halyk.maqsat.budget.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import kz.halyk.maqsat.budget.domain.BudgetCategory;
import kz.halyk.maqsat.budget.domain.BudgetPlan;
import kz.halyk.maqsat.budget.domain.OwnerType;
import kz.halyk.maqsat.budget.dto.CreatePlanRequest;
import kz.halyk.maqsat.budget.dto.DashboardResponse;
import kz.halyk.maqsat.budget.dto.DashboardResponse.CategoryView;
import kz.halyk.maqsat.budget.exception.PlanNotFoundException;
import kz.halyk.maqsat.budget.repository.BudgetPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetService {

    private final BudgetPlanRepository planRepository;
    private final MeterRegistry meterRegistry;

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

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(String currentUserId) {
        BudgetPlan plan = activePlan(currentUserId, OwnerType.USER, LocalDate.now())
                .orElseThrow(() -> new PlanNotFoundException("No active budget plan for the current user"));

        BigDecimal totalLimit = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;
        var views = new java.util.ArrayList<CategoryView>();
        for (BudgetCategory c : plan.getCategories()) {
            BigDecimal remaining = c.getLimitAmount().subtract(c.getSpentAmount());
            views.add(new CategoryView(c.getName(), c.getType().name(), c.getLimitAmount(), c.getSpentAmount(), remaining));
            totalLimit = totalLimit.add(c.getLimitAmount());
            totalSpent = totalSpent.add(c.getSpentAmount());
        }
        return new DashboardResponse(plan.getId(), plan.getPeriodStart(), plan.getPeriodEnd(), totalLimit, totalSpent, views);
    }

    /**
     * Apply a categorized transaction to the owner's active plan, incrementing the matching category.
     * No-op when there is no active plan or no matching category.
     */
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

    private Optional<BudgetPlan> activePlan(String ownerId, OwnerType ownerType, LocalDate date) {
        return planRepository
                .findFirstByOwnerIdAndOwnerTypeAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqualOrderByPeriodStartDesc(
                        ownerId, ownerType, date, date);
    }
}