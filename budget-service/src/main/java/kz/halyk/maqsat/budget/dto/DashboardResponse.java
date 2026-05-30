package kz.halyk.maqsat.budget.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DashboardResponse(
        UUID planId,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal totalLimit,
        BigDecimal totalSpent,
        List<CategoryView> categories
) {
    public record CategoryView(
            String name,
            String type,
            BigDecimal limit,
            BigDecimal spent,
            BigDecimal remaining
    ) {
    }
}