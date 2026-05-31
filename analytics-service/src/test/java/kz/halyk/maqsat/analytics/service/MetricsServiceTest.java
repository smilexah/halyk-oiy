package kz.halyk.maqsat.analytics.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.analytics.client.BudgetClient;
import kz.halyk.maqsat.analytics.client.BudgetClient.ActivePlan;
import kz.halyk.maqsat.analytics.client.BudgetClient.CategoryLimit;
import kz.halyk.maqsat.analytics.domain.CategoryStat;
import kz.halyk.maqsat.analytics.domain.DriftReport;
import kz.halyk.maqsat.analytics.domain.TxnFact;
import kz.halyk.maqsat.analytics.domain.UserMetrics;
import kz.halyk.maqsat.analytics.repository.CategoryStatRepository;
import kz.halyk.maqsat.analytics.repository.DriftReportRepository;
import kz.halyk.maqsat.analytics.repository.RecurringDebitRepository;
import kz.halyk.maqsat.analytics.repository.TxnFactRepository;
import kz.halyk.maqsat.analytics.repository.UserMetricsRepository;
import kz.halyk.maqsat.common.event.EventTopics;
import kz.halyk.maqsat.common.event.MetricsComputed;
import kz.halyk.maqsat.common.event.PlanDriftDetected;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests for MetricsService + DriftService using H2 in-memory database.
 * KafkaTemplate and BudgetClient are mocked with {@code @MockBean}.
 *
 * <p>Flyway is disabled in the test profile; Hibernate uses {@code create-drop} DDL.
 * The {@code DriftReport.driftByCategory} field uses {@code @JdbcTypeCode(SqlTypes.JSON)},
 * which H2 maps to its native JSON type.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({MetricsService.class, DriftService.class})
class MetricsServiceTest {

    @MockBean
    @SuppressWarnings("rawtypes")
    KafkaTemplate kafkaTemplate;

    @MockBean
    BudgetClient budgetClient;

    @Autowired
    MetricsService metricsService;

    @Autowired
    TxnFactRepository txnRepo;

    @Autowired
    UserMetricsRepository metricsRepo;

    @Autowired
    CategoryStatRepository catRepo;

    @Autowired
    RecurringDebitRepository recRepo;

    @Autowired
    DriftReportRepository driftRepo;

    @BeforeEach
    void setUp() {
        driftRepo.deleteAll();
        catRepo.deleteAll();
        recRepo.deleteAll();
        metricsRepo.deleteAll();
        txnRepo.deleteAll();
        // Default: no active plan
        when(budgetClient.fetchActivePlan(anyString())).thenReturn(null);
    }

    // ------------------------------------------------------------------
    // Test 1: Empty period → zeros stored + event published
    // ------------------------------------------------------------------
    @Test
    void emptyPeriod_storesZerosAndPublishesEvent() {
        UserMetrics m = metricsService.computeForUser("user-empty", "2026-01");

        assertThat(m.getTotalSpent()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(m.getIncomeEstimate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metricsRepo.findByUserIdAndPeriod("user-empty", "2026-01")).isPresent();

        verify(kafkaTemplate, atLeastOnce()).send(
                eq(EventTopics.ANALYTICS_METRICS_COMPUTED), eq("user-empty"), any(MetricsComputed.class));
    }

    // ------------------------------------------------------------------
    // Test 2: Three expenses same category → correct CategoryStat
    // ------------------------------------------------------------------
    @Test
    void threeExpenses_sameCategoryStatCorrect() {
        String userId = "user-cat";
        String period = "2026-05";

        saveTxn(userId, "2026-05-05T10:00:00Z", new BigDecimal("1000"), "DEBIT", "Продукты");
        saveTxn(userId, "2026-05-15T10:00:00Z", new BigDecimal("2000"), "DEBIT", "Продукты");
        saveTxn(userId, "2026-05-25T10:00:00Z", new BigDecimal("3000"), "DEBIT", "Продукты");

        metricsService.computeForUser(userId, period);

        List<CategoryStat> stats = catRepo.findByUserIdAndPeriod(userId, period);
        assertThat(stats).hasSize(1);
        CategoryStat stat = stats.get(0);
        assertThat(stat.getCategoryName()).isEqualTo("Продукты");
        assertThat(stat.getTxnCount()).isEqualTo(3);
        assertThat(stat.getTotalAmount()).isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(stat.getAvgAmount()).isEqualByComparingTo(new BigDecimal("2000.00"));

        UserMetrics m = metricsRepo.findByUserIdAndPeriod(userId, period).orElseThrow();
        assertThat(m.getTotalSpent()).isEqualByComparingTo(new BigDecimal("6000.00"));
    }

    // ------------------------------------------------------------------
    // Test 3: Recurring detection — 3 × 990 on distinct days
    // ------------------------------------------------------------------
    @Test
    void recurringDetection_threeMatchingAmountsOnDistinctDays() {
        String userId = "user-rec";
        String period = "2026-05";

        saveTxn(userId, "2026-05-01T10:00:00Z", new BigDecimal("990"), "DEBIT", "Подписки");
        saveTxn(userId, "2026-05-08T10:00:00Z", new BigDecimal("990"), "DEBIT", "Подписки");
        saveTxn(userId, "2026-05-15T10:00:00Z", new BigDecimal("990"), "DEBIT", "Подписки");

        metricsService.computeForUser(userId, period);

        var recurring = recRepo.findByUserId(userId);
        assertThat(recurring).hasSize(1);
        var rec = recurring.get(0);
        assertThat(rec.getAmount()).isEqualByComparingTo(new BigDecimal("990.00"));
        assertThat(rec.getConfidence()).isLessThanOrEqualTo(BigDecimal.ONE);
    }

    // ------------------------------------------------------------------
    // Test 4: Drift detection — Продукты over budget by 30%
    // ------------------------------------------------------------------
    @Test
    void driftDetection_overBudgetByThirtyPercent() {
        String userId = "user-drift";
        String period = "2026-05";

        // Seed 130,000 in Продукты
        saveTxn(userId, "2026-05-10T10:00:00Z", new BigDecimal("130000"), "DEBIT", "Продукты");

        // Mock active plan: Продукты limit = 100,000
        UUID planId = UUID.randomUUID();
        when(budgetClient.fetchActivePlan(userId)).thenReturn(
                new ActivePlan(planId, "2026-05",
                        List.of(new CategoryLimit("Продукты", new BigDecimal("100000"), "EXPENSE"))));

        metricsService.computeForUser(userId, period);

        var reportOpt = driftRepo.findTopByUserIdAndPeriodOrderByComputedAtDesc(userId, period);
        assertThat(reportOpt).isPresent();
        DriftReport report = reportOpt.get();
        assertThat(report.isMatches()).isFalse();
        assertThat(report.isRecommendAdjustment()).isTrue();
        assertThat(report.getDriftByCategory()).containsKey("Продукты");

        BigDecimal drift = report.getDriftByCategory().get("Продукты");
        // ratio = (130000 - 100000) / 100000 = 0.30
        assertThat(drift).isEqualByComparingTo(new BigDecimal("0.3000"));

        verify(kafkaTemplate, atLeastOnce()).send(
                eq(EventTopics.ANALYTICS_PLAN_DRIFT_DETECTED), eq(userId), any(PlanDriftDetected.class));
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------
    private void saveTxn(String userId, String occurredAt, BigDecimal amount,
            String direction, String categoryName) {
        TxnFact t = new TxnFact();
        t.setTransactionId(UUID.randomUUID());
        t.setUserId(userId);
        t.setAccountId("acc-1");
        t.setAmount(amount);
        t.setCategoryName(categoryName);
        t.setOccurredAt(Instant.parse(occurredAt));
        t.setDirection(direction);
        t.setCurrency("KZT");
        txnRepo.save(t);
    }
}
