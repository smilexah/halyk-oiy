package kz.halyk.maqsat.ai.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

public record GenerateBudgetPlanRequest(
        @NotNull @Positive BigDecimal monthlyIncome,
        List<CategorySpend> recentSpending
) {}
