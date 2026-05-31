package kz.halyk.maqsat.parsebudget.dto;

import java.util.List;

public record BudgetPlanProposal(List<PlannedCategory> categories, String rationale) {}
