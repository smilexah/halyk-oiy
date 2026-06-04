package kz.halyk.maqsat.budget.dto.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import kz.halyk.maqsat.budget.entities.enums.CategoryType;
import kz.halyk.maqsat.budget.entities.enums.OwnerType;

public record ParsedPlanPayload(
        OwnerType ownerType,
        String ownerId,
        String period,
        @NotEmpty @Valid List<PlannedCategory> categories,
        boolean createdByAi,
        String rationale
) {
    public record PlannedCategory(
            @NotNull String name,
            @NotNull CategoryType type,
            @NotNull BigDecimal limitAmount
    ) {}
}
