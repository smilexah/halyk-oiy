package kz.halyk.maqsat.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record RecommendationBatch(
        String userId,
        String period,
        List<Item> items
) {
    public record Item(
            String offerId,
            String partner,
            BigDecimal score,
            List<String> audienceTags,
            String rationale
    ) {}
}
