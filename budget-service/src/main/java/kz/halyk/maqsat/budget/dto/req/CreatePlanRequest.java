package kz.halyk.maqsat.budget.dto.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import kz.halyk.maqsat.budget.entities.enums.OwnerType;

public record CreatePlanRequest(
        OwnerType ownerType,
        String ownerId,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd,
        @NotEmpty @Valid List<CategoryRequest> categories
) {}
