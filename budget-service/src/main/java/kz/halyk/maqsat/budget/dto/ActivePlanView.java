package kz.halyk.maqsat.budget.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Lightweight projection of an active budget plan returned by the internal
 * analytics endpoint {@code GET /api/budget/internal/active/{userId}}.
 */
public record ActivePlanView(
        UUID planId,
        String period,
        List<CategoryLimitView> categories
) {
    public record CategoryLimitView(
            String name,
            BigDecimal limit,
            String type
    ) {}
}
