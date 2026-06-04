package kz.halyk.maqsat.recommendation.dto.res;

import java.math.BigDecimal;
import java.util.Set;

public record AudienceProfile(
        String userId,
        Set<String> tags,
        BigDecimal incomeEstimate,
        BigDecimal savingsRate
) {}
