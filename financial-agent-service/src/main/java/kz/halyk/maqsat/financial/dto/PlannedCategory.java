package kz.halyk.maqsat.financial.dto;

import java.math.BigDecimal;

public record PlannedCategory(String name, String type, BigDecimal limitAmount) {}
