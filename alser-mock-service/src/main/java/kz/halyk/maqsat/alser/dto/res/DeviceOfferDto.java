package kz.halyk.maqsat.alser.dto.res;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DeviceOfferDto(
        UUID id,
        String sku,
        String name,
        BigDecimal basePrice,
        BigDecimal discountPct,
        List<String> audienceTags,
        Instant validUntil
) {}
