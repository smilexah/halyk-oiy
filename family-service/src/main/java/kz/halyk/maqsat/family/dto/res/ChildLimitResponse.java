package kz.halyk.maqsat.family.dto.res;

import java.math.BigDecimal;
import java.time.Instant;

public record ChildLimitResponse(
        String userId,
        BigDecimal dailyLimit,
        Instant overrideUntil,
        BigDecimal overrideAmount
) {}
