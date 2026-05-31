package kz.halyk.maqsat.parsebudget.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;

public record ReplanRequest(
        @NotBlank String userId,
        @NotBlank String period,
        @NotNull @Valid BudgetPlanProposal proposal,
        Map<String, BigDecimal> driftSnapshot
) {}
