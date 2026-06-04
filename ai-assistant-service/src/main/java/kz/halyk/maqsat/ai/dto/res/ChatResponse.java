package kz.halyk.maqsat.ai.dto.res;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatResponse(String reply, BudgetPlanResult updatedPlan) {}
