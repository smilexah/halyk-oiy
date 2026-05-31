package kz.halyk.maqsat.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.analytics.domain.CategoryStat;
import kz.halyk.maqsat.analytics.domain.RecurringDebit;
import kz.halyk.maqsat.analytics.domain.UserMetrics;

public record UserMetricsResponse(
        UUID id,
        String userId,
        String period,
        BigDecimal incomeEstimate,
        BigDecimal totalSpent,
        BigDecimal avgTransaction,
        BigDecimal medianTransaction,
        BigDecimal volatility,
        BigDecimal savingsRate,
        Instant computedAt,
        List<CategoryStatView> categories,
        List<RecurringDebitView> recurring
) {
    public record CategoryStatView(
            UUID id,
            String categoryName,
            int txnCount,
            BigDecimal totalAmount,
            BigDecimal avgAmount,
            BigDecimal medianAmount
    ) {}

    public record RecurringDebitView(
            UUID id,
            String label,
            BigDecimal amount,
            Integer dayOfMonth,
            BigDecimal confidence,
            Instant lastSeenAt
    ) {}

    public static UserMetricsResponse from(UserMetrics m, List<CategoryStat> cats,
            List<RecurringDebit> rec) {
        List<CategoryStatView> catViews = cats.stream()
                .map(c -> new CategoryStatView(c.getId(), c.getCategoryName(), c.getTxnCount(),
                        c.getTotalAmount(), c.getAvgAmount(), c.getMedianAmount()))
                .toList();
        List<RecurringDebitView> recViews = rec.stream()
                .map(r -> new RecurringDebitView(r.getId(), r.getLabel(), r.getAmount(),
                        r.getDayOfMonth(), r.getConfidence(), r.getLastSeenAt()))
                .toList();
        return new UserMetricsResponse(
                m.getId(), m.getUserId(), m.getPeriod(),
                m.getIncomeEstimate(), m.getTotalSpent(),
                m.getAvgTransaction(), m.getMedianTransaction(),
                m.getVolatility(), m.getSavingsRate(), m.getComputedAt(),
                catViews, recViews);
    }
}
