package kz.halyk.maqsat.common.event;

import java.math.BigDecimal;
import java.time.Instant;

public record MetricsComputed(
        String userId,
        String period,
        BigDecimal incomeEstimate,
        BigDecimal totalSpent,
        BigDecimal volatility,
        BigDecimal savingsRate,
        Instant computedAt
) {}
