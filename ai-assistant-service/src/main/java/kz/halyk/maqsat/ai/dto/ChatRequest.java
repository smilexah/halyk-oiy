package kz.halyk.maqsat.ai.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ChatRequest(
        BudgetPlanResult currentPlan,
        List<ChatMessage> history,
        @NotBlank String message
) {
}
