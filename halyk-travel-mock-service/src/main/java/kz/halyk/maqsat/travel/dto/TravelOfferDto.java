package kz.halyk.maqsat.travel.dto;

import kz.halyk.maqsat.travel.domain.TravelOffer;

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
) {
    public static TravelOfferDto from(TravelOffer offer) {
        return new TravelOfferDto(
                offer.getId(),
                offer.getKind().name(),
                offer.getDestination(),
                offer.getBasePrice(),
                offer.getDiscountPct(),
                offer.getAudienceTags(),
                offer.getValidUntil()
        );
    }
}
