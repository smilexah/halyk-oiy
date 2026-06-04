package kz.halyk.maqsat.analytics.dto.res;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DriftReportResponse(
        UUID id,
        String userId,
        UUID planId,
        String period,
        boolean matches,
        Map<String, BigDecimal> driftByCategory,
        boolean recommendAdjustment,
        Instant computedAt
) {}
