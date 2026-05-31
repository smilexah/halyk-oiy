package kz.halyk.maqsat.financial.dto;

import java.util.List;

public record BudgetPlanProposal(List<PlannedCategory> categories, String rationale) {}
