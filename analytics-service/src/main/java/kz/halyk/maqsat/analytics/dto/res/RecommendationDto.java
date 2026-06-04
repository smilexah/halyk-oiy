package kz.halyk.maqsat.analytics.dto.res;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecommendationDto(
        UUID id,
        String userId,
        String period,
        String offerId,
        String partner,
        BigDecimal score,
        List<String> audienceTags,
        String rationale,
        Instant createdAt
) {}
