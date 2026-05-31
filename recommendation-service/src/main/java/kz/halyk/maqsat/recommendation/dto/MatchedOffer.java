package kz.halyk.maqsat.recommendation.dto;

import java.math.BigDecimal;
import java.util.List;

public record MatchedOffer(
        String offerId,
        String partner,
        BigDecimal score,
        List<String> audienceTags,
        String rationale
) {}
