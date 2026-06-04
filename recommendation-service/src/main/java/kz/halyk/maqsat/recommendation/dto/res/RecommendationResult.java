package kz.halyk.maqsat.recommendation.dto.res;

import java.time.Instant;
import java.util.List;

public record RecommendationResult(
        String userId,
        String period,
        List<MatchedOffer> offers,
        Instant generatedAt
) {}
