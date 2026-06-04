package kz.halyk.maqsat.financial.dto.res;

import java.math.BigDecimal;

public record PlannedCategory(String name, String type, BigDecimal limitAmount) {}
