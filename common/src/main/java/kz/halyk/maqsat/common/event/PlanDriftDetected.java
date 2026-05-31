package kz.halyk.maqsat.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PlanDriftDetected(
        String userId,
        UUID planId,
        String period,
        Map<String, BigDecimal> driftByCategory,
        boolean recommendAdjustment,
        Instant computedAt
) {}
