package kz.halyk.maqsat.alser.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.alser.domain.DeviceOffer;

public record DeviceOfferDto(
        UUID id,
        String sku,
        String name,
        BigDecimal basePrice,
        BigDecimal discountPct,
        List<String> audienceTags,
        Instant validUntil
) {
    public static DeviceOfferDto from(DeviceOffer offer) {
        return new DeviceOfferDto(
                offer.getId(),
                offer.getSku(),
                offer.getName(),
                offer.getBasePrice(),
                offer.getDiscountPct(),
                offer.getAudienceTags(),
                offer.getValidUntil()
        );
    }
}
