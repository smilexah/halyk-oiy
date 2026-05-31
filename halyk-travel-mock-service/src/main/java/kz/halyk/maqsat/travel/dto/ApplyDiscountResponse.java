package kz.halyk.maqsat.travel.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ApplyDiscountResponse(
        UUID id,
        UUID offerId,
        String userId,
        BigDecimal basePrice,
        BigDecimal discountPct,
        BigDecimal finalPrice,
        Instant bookedAt
) {
}
