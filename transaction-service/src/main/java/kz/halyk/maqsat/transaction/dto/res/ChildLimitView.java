package kz.halyk.maqsat.transaction.dto.res;

import java.math.BigDecimal;
import java.time.Instant;

public record ChildLimitView(
        String userId,
        BigDecimal dailyLimit,
        Instant overrideUntil,
        BigDecimal overrideAmount
) {}
