package kz.halyk.maqsat.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Published on topic {@link EventTopics#AI_RECOMMENDATION_READY} after the recommendation-service
 *  matches partner offers to a user's audience profile for a period. */
public record RecommendationReady(
        String userId,
        String period,
        List<MatchedOffer> offers,
        Instant generatedAt
) {
    public record MatchedOffer(
            String offerId,
            String partner,
            BigDecimal score,
            List<String> audienceTags,
            String rationale
    ) {
    }
}
