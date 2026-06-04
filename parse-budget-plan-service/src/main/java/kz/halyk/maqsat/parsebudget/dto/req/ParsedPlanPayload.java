package kz.halyk.maqsat.parsebudget.dto.req;

import java.math.BigDecimal;
import java.util.List;

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
