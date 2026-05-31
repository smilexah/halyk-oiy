package kz.halyk.maqsat.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import kz.halyk.maqsat.analytics.domain.DriftReport;

public record DriftReportResponse(
        UUID id,
        String userId,
        UUID planId,
        String period,
        boolean matches,
        Map<String, BigDecimal> driftByCategory,
        boolean recommendAdjustment,
        Instant computedAt
) {
    public static DriftReportResponse from(DriftReport r) {
        return new DriftReportResponse(
                r.getId(), r.getUserId(), r.getPlanId(), r.getPeriod(),
                r.isMatches(), r.getDriftByCategory(), r.isRecommendAdjustment(),
                r.getComputedAt());
    }
}
