package kz.halyk.maqsat.ai.controller;

import jakarta.validation.Valid;
import kz.halyk.maqsat.ai.dto.BudgetPlanResult;
import kz.halyk.maqsat.ai.dto.ChatRequest;
import kz.halyk.maqsat.ai.dto.ChatResponse;
import kz.halyk.maqsat.ai.dto.GenerateBudgetPlanRequest;
import kz.halyk.maqsat.ai.service.AiAssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiAssistantService service;

    @PostMapping("/budget-plan")
    public BudgetPlanResult budgetPlan(@Valid @RequestBody GenerateBudgetPlanRequest request) {
        return service.generatePlan(request);
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return service.chat(request);
    }
}
