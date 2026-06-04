package kz.halyk.maqsat.ai.dto.req;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import kz.halyk.maqsat.ai.dto.res.BudgetPlanResult;

public record ChatRequest(
        BudgetPlanResult currentPlan,
        List<ChatMessage> history,
        @NotBlank String message
) {}
