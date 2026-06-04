package kz.halyk.maqsat.ai.services;

import kz.halyk.maqsat.ai.dto.req.ChatRequest;
import kz.halyk.maqsat.ai.dto.req.GenerateBudgetPlanRequest;
import kz.halyk.maqsat.ai.dto.res.BudgetPlanResult;
import kz.halyk.maqsat.ai.dto.res.ChatResponse;

public interface AiAssistantService {
    BudgetPlanResult generatePlan(GenerateBudgetPlanRequest request);
    ChatResponse chat(ChatRequest request);
}
