package kz.halyk.maqsat.financial.dto.res;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserMetricsDto(
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
}
