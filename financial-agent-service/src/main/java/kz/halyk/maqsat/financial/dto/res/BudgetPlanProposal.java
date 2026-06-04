package kz.halyk.maqsat.financial.dto.res;

import java.util.List;

public record BudgetPlanProposal(List<PlannedCategory> categories, String rationale) {}
