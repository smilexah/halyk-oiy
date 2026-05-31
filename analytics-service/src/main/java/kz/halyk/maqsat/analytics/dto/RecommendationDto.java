package kz.halyk.maqsat.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.analytics.domain.Recommendation;

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
) {
    public static RecommendationDto from(Recommendation r) {
        return new RecommendationDto(
                r.getId(),
                r.getUserId(),
                r.getPeriod(),
                r.getOfferId(),
                r.getPartner(),
                r.getScore(),
                r.getAudienceTags(),
                r.getRationale(),
                r.getCreatedAt()
        );
    }
}
