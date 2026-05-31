package kz.halyk.maqsat.summary.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Local mirror of budget-service's ActivePlanView, to avoid cross-service coupling. */
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
