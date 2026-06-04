package kz.halyk.maqsat.budget.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import kz.halyk.maqsat.budget.entities.enums.CategoryType;

public record CategoryRequest(
        @NotBlank String name,
        @NotNull CategoryType type,
        @NotNull @Positive BigDecimal limitAmount
) {}
