package kz.halyk.maqsat.family.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Returned to transaction-service so it can enforce a child's daily limit. */
public record ChildLimitResponse(
        String userId,
        BigDecimal dailyLimit,
        Instant overrideUntil,
        BigDecimal overrideAmount
) {
}
