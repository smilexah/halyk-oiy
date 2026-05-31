package kz.halyk.maqsat.parsebudget.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payload sent to budget-service {@code POST /api/budget/internal/replan/{userId}}.
 * Field types match the JSON shape budget-service expects: ownerType and category type
 * are plain strings that budget-service deserializes into its own enums.
 */
public record ParsedPlanPayload(
        String ownerType,
        String ownerId,
        String period,
        List<PlannedCategory> categories,
        boolean createdByAi,
        String rationale
) {
    public record PlannedCategory(String name, String type, BigDecimal limitAmount) {}
}
