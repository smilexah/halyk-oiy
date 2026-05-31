package kz.halyk.maqsat.travel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ApplyDiscountRequest(
        @NotBlank String userId,
        @NotNull UUID offerId
) {
}
