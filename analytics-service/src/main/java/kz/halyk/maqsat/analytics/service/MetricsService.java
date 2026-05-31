package kz.halyk.maqsat.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kz.halyk.maqsat.analytics.domain.CategoryStat;
import kz.halyk.maqsat.analytics.domain.RecurringDebit;
import kz.halyk.maqsat.analytics.domain.TxnFact;
import kz.halyk.maqsat.analytics.domain.UserMetrics;
import kz.halyk.maqsat.analytics.repository.CategoryStatRepository;
import kz.halyk.maqsat.analytics.repository.RecurringDebitRepository;
import kz.halyk.maqsat.analytics.repository.TxnFactRepository;
import kz.halyk.maqsat.analytics.repository.UserMetricsRepository;
import kz.halyk.maqsat.common.event.EventTopics;
import kz.halyk.maqsat.common.event.MetricsComputed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsService {

    private final TxnFactRepository txnRepo;
    private final UserMetricsRepository metricsRepo;
    private final CategoryStatRepository catRepo;
    private final RecurringDebitRepository recRepo;
    private final DriftService driftService;
    private final KafkaTemplate<String, Object> producer;

    /**
     * Computes user metrics for the given period (YYYY-MM), persists rollup rows,
     * emits a {@code MetricsComputed} Kafka event, then triggers drift detection.
     *
     * @param userId the user identifier
     * @param period YYYY-MM period string
     * @return the persisted {@link UserMetrics}
     */
    @Transactional
    public UserMetrics computeForUser(String userId, String period) {
        Instant[] range = monthRange(period);
        Instant start = range[0];
        Instant end = range[1];

        List<TxnFact> txns = txnRepo.findByUserIdAndOccurredAtBetween(userId, start, end);

        // Separate expenses and incomes
        List<TxnFact> expenses = txns.stream()
                .filter(t -> isExpense(t))
                .collect(Collectors.toList());
        List<TxnFact> incomes = txns.stream()
                .filter(t -> isIncome(t))
                .collect(Collectors.toList());

        BigDecimal totalSpent = expenses.stream()
                .map(TxnFact::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal incomeEstimate = incomes.stream()
                .map(TxnFact::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgTransaction = BigDecimal.ZERO;
        BigDecimal medianTransaction = BigDecimal.ZERO;
        BigDecimal volatility = BigDecimal.ZERO;
        BigDecimal savingsRate = BigDecimal.ZERO;

        if (!expenses.isEmpty()) {
            List<BigDecimal> amounts = expenses.stream()
                    .map(TxnFact::getAmount)
                    .collect(Collectors.toList());

            BigDecimal sum = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            avgTransaction = sum.divide(BigDecimal.valueOf(amounts.size()), 2, RoundingMode.HALF_UP);
            medianTransaction = median(amounts);
            volatility = computeVolatility(amounts).setScale(4, RoundingMode.HALF_UP);
        }

        if (incomeEstimate.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = incomeEstimate.subtract(totalSpent);
            savingsRate = diff.max(BigDecimal.ZERO)
                    .divide(incomeEstimate, 4, RoundingMode.HALF_UP);
        }

        // Upsert user_metrics
        UserMetrics metrics = metricsRepo.findByUserIdAndPeriod(userId, period).orElse(null);
        if (metrics == null) {
            metrics = new UserMetrics();
            metrics.setUserId(userId);
            metrics.setPeriod(period);
        }
        metrics.setTotalSpent(totalSpent);
        metrics.setIncomeEstimate(incomeEstimate);
        metrics.setAvgTransaction(avgTransaction);
        metrics.setMedianTransaction(medianTransaction);
        metrics.setVolatility(volatility);
        metrics.setSavingsRate(savingsRate);
        metrics.setComputedAt(Instant.now());
        metrics = metricsRepo.save(metrics);

        // Category rollup — replace existing rows
        catRepo.deleteByUserIdAndPeriod(userId, period);
        List<CategoryStat> stats = buildCategoryStats(userId, period, expenses);
        catRepo.saveAll(stats);

        // Recurring debit detection
        detectAndUpsertRecurring(userId, expenses);

        // Publish event
        MetricsComputed event = new MetricsComputed(
                userId, period,
                incomeEstimate, totalSpent,
                volatility, savingsRate,
                metrics.getComputedAt());
        producer.send(EventTopics.ANALYTICS_METRICS_COMPUTED, userId, event);
        log.info("MetricsComputed published for userId={} period={}", userId, period);

        // Drift detection — isolated so a budget-service outage doesn't roll back metrics
        try {
            driftService.detect(userId, period);
        } catch (Exception ex) {
            log.warn("Drift detection failed for userId={} period={} — metrics committed anyway: {}",
                    userId, period, ex.getMessage());
        }

        return metrics;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static boolean isExpense(TxnFact t) {
        if ("CREDIT".equalsIgnoreCase(t.getDirection())) {
            return false;
        }
        String op = t.getOperationType();
        if ("SALARY".equalsIgnoreCase(op) || "TOPUP".equalsIgnoreCase(op)) {
            return false;
        }
        return true;
    }

    private static boolean isIncome(TxnFact t) {
        if ("CREDIT".equalsIgnoreCase(t.getDirection())) {
            return true;
        }
        String op = t.getOperationType();
        return "SALARY".equalsIgnoreCase(op) || "TOPUP".equalsIgnoreCase(op);
    }

    private static Instant[] monthRange(String period) {
        YearMonth ym = YearMonth.parse(period);
        Instant start = ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return new Instant[]{start, end};
    }

    private static BigDecimal median(List<BigDecimal> values) {
        if (values.isEmpty()) return BigDecimal.ZERO;
        List<BigDecimal> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        }
        return sorted.get(n / 2 - 1).add(sorted.get(n / 2))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal computeVolatility(List<BigDecimal> amounts) {
        if (amounts.size() < 2) return BigDecimal.ZERO;
        BigDecimal sum = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mean = sum.divide(BigDecimal.valueOf(amounts.size()), 10, RoundingMode.HALF_UP);
        if (mean.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        // variance = sum((x - mean)^2) / n
        BigDecimal variance = amounts.stream()
                .map(a -> a.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(amounts.size()), 10, RoundingMode.HALF_UP);
        double stddev = Math.sqrt(variance.doubleValue());
        double vol = stddev / mean.doubleValue();
        return BigDecimal.valueOf(vol).setScale(4, RoundingMode.HALF_UP);
    }

    private static List<CategoryStat> buildCategoryStats(String userId, String period,
            List<TxnFact> expenses) {
        Map<String, List<BigDecimal>> grouped = new HashMap<>();
        for (TxnFact t : expenses) {
            grouped.computeIfAbsent(t.getCategoryName(), k -> new ArrayList<>()).add(t.getAmount());
        }
        List<CategoryStat> stats = new ArrayList<>();
        for (Map.Entry<String, List<BigDecimal>> entry : grouped.entrySet()) {
            List<BigDecimal> amounts = entry.getValue();
            BigDecimal sum = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avg = sum.divide(BigDecimal.valueOf(amounts.size()), 2, RoundingMode.HALF_UP);
            BigDecimal med = median(amounts);

            CategoryStat stat = new CategoryStat();
            stat.setUserId(userId);
            stat.setPeriod(period);
            stat.setCategoryName(entry.getKey());
            stat.setTxnCount(amounts.size());
            stat.setTotalAmount(sum);
            stat.setAvgAmount(avg);
            stat.setMedianAmount(med);
            stats.add(stat);
        }
        return stats;
    }

    private void detectAndUpsertRecurring(String userId, List<TxnFact> expenses) {
        // Group by rounded amount (long)
        Map<Long, List<TxnFact>> byAmount = new HashMap<>();
        for (TxnFact t : expenses) {
            long key = Math.round(t.getAmount().doubleValue());
            byAmount.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }

        for (Map.Entry<Long, List<TxnFact>> entry : byAmount.entrySet()) {
            List<TxnFact> group = entry.getValue();
            if (group.size() < 3) continue;

            // Distinct days of month
            long distinctDays = group.stream()
                    .map(t -> t.getOccurredAt().atZone(ZoneOffset.UTC).getDayOfMonth())
                    .distinct()
                    .count();
            if (distinctDays < 3) continue;

            // Average amount
            BigDecimal avgAmt = group.stream()
                    .map(TxnFact::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(group.size()), 2, RoundingMode.HALF_UP);

            // Mode of days of month
            Map<Integer, Long> dayCounts = group.stream()
                    .collect(Collectors.groupingBy(
                            t -> t.getOccurredAt().atZone(ZoneOffset.UTC).getDayOfMonth(),
                            Collectors.counting()));
            int modeDay = dayCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(1);

            // Confidence: min(1.0, count / 6.0)
            BigDecimal confidence = BigDecimal.valueOf(Math.min(1.0, group.size() / 6.0))
                    .setScale(3, RoundingMode.HALF_UP);

            // Last seen
            Instant lastSeen = group.stream()
                    .map(TxnFact::getOccurredAt)
                    .max(Instant::compareTo)
                    .orElse(Instant.now());

            // Label: first txn's categoryName, or first 64 chars of details
            TxnFact first = group.get(0);
            String label;
            if (first.getDetails() != null && !first.getDetails().isBlank()) {
                String det = first.getDetails().trim();
                label = det.length() > 64 ? det.substring(0, 64) : det;
            } else {
                label = first.getCategoryName();
            }

            // Upsert
            RecurringDebit rec = recRepo.findByUserIdAndLabel(userId, label).orElse(null);
            if (rec == null) {
                rec = new RecurringDebit();
                rec.setUserId(userId);
                rec.setLabel(label);
            }
            rec.setAmount(avgAmt);
            rec.setDayOfMonth(modeDay);
            rec.setConfidence(confidence);
            rec.setLastSeenAt(lastSeen);
            recRepo.save(rec);
        }
    }
}
