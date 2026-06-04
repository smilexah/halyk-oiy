package kz.halyk.maqsat.alser.dto.res;

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
        Instant appliedAt
) {}
