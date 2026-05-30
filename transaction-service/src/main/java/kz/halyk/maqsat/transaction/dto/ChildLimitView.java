package kz.halyk.maqsat.transaction.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Daily-limit info for a child, fetched from family-service. */
public record ChildLimitView(
        String userId,
        BigDecimal dailyLimit,
        Instant overrideUntil,
        BigDecimal overrideAmount
) {
}
