package kz.halyk.maqsat.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kz.halyk.maqsat.analytics.client.BudgetClient;
import kz.halyk.maqsat.analytics.client.BudgetClient.ActivePlan;
import kz.halyk.maqsat.analytics.domain.CategoryStat;
import kz.halyk.maqsat.analytics.domain.DriftReport;
import kz.halyk.maqsat.analytics.repository.CategoryStatRepository;
import kz.halyk.maqsat.analytics.repository.DriftReportRepository;
import kz.halyk.maqsat.common.event.EventTopics;
import kz.halyk.maqsat.common.event.PlanDriftDetected;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriftService {

    private final CategoryStatRepository catRepo;
    private final BudgetClient budgetClient;
    private final DriftReportRepository reports;
    private final KafkaTemplate<String, Object> producer;

    @Value("${analytics.drift.threshold:0.15}")
    double threshold;

    /**
     * Compares actual category spending against the user's active budget plan.
     * Persists a {@link DriftReport} and emits a {@code PlanDriftDetected} Kafka event.
     * If no active plan exists, persists a trivial report with {@code matches=true}.
     *
     * @param userId the user identifier
     * @param period YYYY-MM period
     * @return the persisted drift report
     */
    @Transactional
    public DriftReport detect(String userId, String period) {
        ActivePlan plan = budgetClient.fetchActivePlan(userId);

        Map<String, BigDecimal> driftByCategory = new HashMap<>();
        UUID planId;

        if (plan == null || plan.categories() == null || plan.categories().isEmpty()) {
            // No active plan — create a trivial "no drift" report
            planId = UUID.fromString("00000000-0000-0000-0000-000000000000");
            DriftReport trivial = buildReport(userId, planId, period, true, driftByCategory, false);
            DriftReport saved = reports.save(trivial);
            log.info("No active plan for userId={} period={} — trivial drift report persisted", userId, period);
            return saved;
        }

        planId = plan.planId();
        List<CategoryStat> actuals = catRepo.findByUserIdAndPeriod(userId, period);

        for (BudgetClient.CategoryLimit cat : plan.categories()) {
            BigDecimal actual = actuals.stream()
                    .filter(a -> a.getCategoryName().equalsIgnoreCase(cat.name()))
                    .findFirst()
                    .map(CategoryStat::getTotalAmount)
                    .orElse(BigDecimal.ZERO);

            BigDecimal limit = cat.limit();
            BigDecimal divisor = limit.max(BigDecimal.ONE);
            BigDecimal ratio = actual.subtract(limit)
                    .divide(divisor, 4, RoundingMode.HALF_UP);

            if (ratio.abs().doubleValue() > threshold) {
                driftByCategory.put(cat.name(), ratio);
            }
        }

        boolean matches = driftByCategory.isEmpty();
        boolean recommendAdjustment = !matches;

        DriftReport report = buildReport(userId, planId, period, matches, driftByCategory, recommendAdjustment);
        DriftReport saved = reports.save(report);

        PlanDriftDetected event = new PlanDriftDetected(
                userId, planId, period, driftByCategory, recommendAdjustment, saved.getComputedAt());
        producer.send(EventTopics.ANALYTICS_PLAN_DRIFT_DETECTED, userId, event);
        log.info("PlanDriftDetected published for userId={} period={} matches={}", userId, period, matches);

        return saved;
    }

    private static DriftReport buildReport(String userId, UUID planId, String period,
            boolean matches, Map<String, BigDecimal> drift, boolean recommendAdjustment) {
        DriftReport report = new DriftReport();
        report.setUserId(userId);
        report.setPlanId(planId);
        report.setPeriod(period);
        report.setMatches(matches);
        report.setDriftByCategory(drift);
        report.setRecommendAdjustment(recommendAdjustment);
        report.setComputedAt(Instant.now());
        return report;
    }
}
