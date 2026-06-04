package kz.halyk.maqsat.parsebudget.dto.req;

import java.util.List;

public record BudgetPlanProposal(List<PlannedCategory> categories, String rationale) {}
