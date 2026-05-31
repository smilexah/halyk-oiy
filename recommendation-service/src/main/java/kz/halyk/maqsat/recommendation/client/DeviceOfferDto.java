package kz.halyk.maqsat.recommendation.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Local mirror of alser-mock-service's DeviceOfferDto. */
public record DeviceOfferDto(
        UUID id,
        String sku,
        String name,
        BigDecimal basePrice,
        BigDecimal discountPct,
        List<String> audienceTags,
        Instant validUntil
) {}
