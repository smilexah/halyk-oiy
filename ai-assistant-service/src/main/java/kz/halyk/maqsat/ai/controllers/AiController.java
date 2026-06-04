package kz.halyk.maqsat.ai.controllers;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import kz.halyk.maqsat.ai.dto.req.GenerateBudgetPlanRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@Deprecated
@RestController
@RequestMapping("/api/ai")
@Slf4j
public class AiController {

    private final WebClient parseBudgetClient;

    public AiController(WebClient.Builder loadBalancedWebClientBuilder) {
        this.parseBudgetClient = loadBalancedWebClientBuilder
                .baseUrl("lb://parse-budget-plan-service")
                .build();
    }

    @Deprecated
    @PostMapping("/budget-plan")
    public ResponseEntity<Object> budgetPlan(@Valid @RequestBody GenerateBudgetPlanRequest request) {
        log.warn("Deprecated /api/ai/budget-plan called — forwarding to parse-budget-plan-service");
        String period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Map<String, Object> proposal = Map.of("categories", Collections.emptyList(), "rationale", "shim-bypass");
        Map<String, Object> replanRequest = Map.of(
                "userId", "demo-shim", "period", period,
                "proposal", proposal, "driftSnapshot", Collections.emptyMap());
        Object response = parseBudgetClient.post()
                .uri("/api/parse-budget/replan")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(replanRequest)
                .retrieve()
                .bodyToMono(Object.class)
                .block();
        return ResponseEntity.ok(response);
    }

    @Deprecated
    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody(required = false) Object ignored) {
        return ResponseEntity.status(HttpStatus.GONE).body(
                Map.of("error", "endpoint removed — use /api/financial-agent/chat (todo) or /api/parse-budget/replan"));
    }
}
