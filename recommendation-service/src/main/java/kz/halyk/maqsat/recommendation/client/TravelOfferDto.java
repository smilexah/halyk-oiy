package kz.halyk.maqsat.recommendation.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Local mirror of halyk-travel-mock-service's TravelOfferDto. */
public record TravelOfferDto(
        UUID id,
        String kind,
        String destination,
        BigDecimal basePrice,
        BigDecimal discountPct,
        List<String> audienceTags,
        Instant validUntil
) {}
