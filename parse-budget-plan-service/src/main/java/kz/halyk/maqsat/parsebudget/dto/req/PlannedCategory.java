package kz.halyk.maqsat.parsebudget.dto.req;

import java.math.BigDecimal;

public record PlannedCategory(String name, String type, BigDecimal limitAmount) {}
