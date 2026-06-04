package kz.halyk.maqsat.travel.dto.res;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TravelOfferDto(
        UUID id,
        String kind,
        String destination,
        BigDecimal basePrice,
        BigDecimal discountPct,
        List<String> audienceTags,
        Instant validUntil
) {}
