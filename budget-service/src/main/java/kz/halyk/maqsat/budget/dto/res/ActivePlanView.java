package kz.halyk.maqsat.budget.dto.res;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ActivePlanView(
        UUID planId,
        String period,
        List<CategoryLimitView> categories
) {
    public record CategoryLimitView(String name, BigDecimal limit, String type) {}
}
