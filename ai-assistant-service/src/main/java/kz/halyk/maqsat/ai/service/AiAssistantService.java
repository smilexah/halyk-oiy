package kz.halyk.maqsat.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kz.halyk.maqsat.ai.client.AnthropicClient;
import kz.halyk.maqsat.ai.config.AnthropicProperties;
import kz.halyk.maqsat.ai.dto.BudgetPlanResult;
import kz.halyk.maqsat.ai.dto.ChatMessage;
import kz.halyk.maqsat.ai.dto.ChatRequest;
import kz.halyk.maqsat.ai.dto.ChatResponse;
import kz.halyk.maqsat.ai.dto.CategorySpend;
import kz.halyk.maqsat.ai.dto.GenerateBudgetPlanRequest;
import kz.halyk.maqsat.ai.dto.PlannedCategory;
import kz.halyk.maqsat.ai.exception.AiResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAssistantService {

    private static final Set<String> VALID_TYPES = Set.of("MANDATORY", "DISCRETIONARY");

    private static final String PLAN_SYSTEM = """
            You are a budgeting assistant for the Halyk SuperApp (Kazakhstan).
            Split the user's monthly income into spending categories.
            Use ONLY these Russian category names: Продукты, Коммуналка, Транспорт, Такси, Рестораны, Развлечения, Прочее.
            Each category type is MANDATORY or DISCRETIONARY.
            Respond with STRICT JSON only, no markdown, matching exactly:
            {"categories":[{"name":"...","type":"MANDATORY|DISCRETIONARY","limitAmount":number}],"rationale":"..."}
            The sum of limitAmount must not exceed the monthly income.
            """;

    private static final String CHAT_SYSTEM = """
            You are a budgeting assistant editing an existing budget plan through conversation.
            Apply the user's request to the current plan.
            Use ONLY these Russian category names: Продукты, Коммуналка, Транспорт, Такси, Рестораны, Развлечения, Прочее.
            Respond with STRICT JSON only, no markdown, matching exactly:
            {"reply":"short human reply","updatedPlan":{"categories":[{"name":"...","type":"MANDATORY|DISCRETIONARY","limitAmount":number}],"rationale":"..."}}
            """;

    private final AnthropicProperties properties;
    private final AnthropicClient anthropicClient;
    private final ObjectMapper objectMapper;

    public BudgetPlanResult generatePlan(GenerateBudgetPlanRequest request) {
        if (!properties.enabled()) {
            log.info("ANTHROPIC_API_KEY not set — returning rule-based fallback plan");
            return fallbackPlan(request.monthlyIncome());
        }
        String userPrompt = buildPlanPrompt(request);
        String raw = anthropicClient.complete(PLAN_SYSTEM, List.of(new ChatMessage("user", userPrompt)));
        BudgetPlanResult result = parse(raw, BudgetPlanResult.class);
        validatePlan(result);
        return result;
    }

    public ChatResponse chat(ChatRequest request) {
        if (!properties.enabled()) {
            return new ChatResponse("AI assistant is disabled (no ANTHROPIC_API_KEY). Plan unchanged.", request.currentPlan());
        }
        List<ChatMessage> conversation = new ArrayList<>();
        try {
            conversation.add(new ChatMessage("user", "Current plan JSON: " + objectMapper.writeValueAsString(request.currentPlan())));
        } catch (Exception e) {
            throw new AiResponseException("Could not serialize current plan", e);
        }
        if (request.history() != null) {
            conversation.addAll(request.history());
        }
        conversation.add(new ChatMessage("user", request.message()));

        String raw = anthropicClient.complete(CHAT_SYSTEM, conversation);
        ChatResponse response = parse(raw, ChatResponse.class);
        if (response.updatedPlan() != null) {
            validatePlan(response.updatedPlan());
        }
        return response;
    }

    // --- helpers -------------------------------------------------------------

    private String buildPlanPrompt(GenerateBudgetPlanRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Monthly income: ").append(request.monthlyIncome()).append(" KZT.\n");
        if (request.recentSpending() != null && !request.recentSpending().isEmpty()) {
            sb.append("Recent spending by category:\n");
            for (CategorySpend s : request.recentSpending()) {
                sb.append("- ").append(s.name()).append(": ").append(s.amount()).append('\n');
            }
        }
        sb.append("Produce a monthly budget plan.");
        return sb.toString();
    }

    private <T> T parse(String raw, Class<T> type) {
        String json = extractJson(raw);
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("Failed to parse AI response as {}: {}", type.getSimpleName(), raw);
            throw new AiResponseException("AI did not return valid JSON for " + type.getSimpleName(), e);
        }
    }

    private String extractJson(String raw) {
        if (raw == null) {
            throw new AiResponseException("AI returned empty response");
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new AiResponseException("No JSON object found in AI response");
        }
        return raw.substring(start, end + 1);
    }

    private void validatePlan(BudgetPlanResult plan) {
        if (plan.categories() == null || plan.categories().isEmpty()) {
            throw new AiResponseException("Plan has no categories");
        }
        for (PlannedCategory c : plan.categories()) {
            if (c.name() == null || c.name().isBlank()) {
                throw new AiResponseException("Category with blank name");
            }
            if (c.type() == null || !VALID_TYPES.contains(c.type())) {
                throw new AiResponseException("Invalid category type: " + c.type());
            }
            if (c.limitAmount() == null || c.limitAmount().signum() < 0) {
                throw new AiResponseException("Invalid limitAmount for " + c.name());
            }
        }
    }

    private BudgetPlanResult fallbackPlan(BigDecimal income) {
        List<PlannedCategory> categories = List.of(
                category("Продукты", "MANDATORY", income, "0.25"),
                category("Коммуналка", "MANDATORY", income, "0.15"),
                category("Транспорт", "MANDATORY", income, "0.10"),
                category("Такси", "DISCRETIONARY", income, "0.05"),
                category("Рестораны", "DISCRETIONARY", income, "0.10"),
                category("Развлечения", "DISCRETIONARY", income, "0.10"),
                category("Прочее", "DISCRETIONARY", income, "0.25"));
        return new BudgetPlanResult(categories, "Rule-based 25/15/10/5/10/10/25 split of monthly income.");
    }

    private PlannedCategory category(String name, String type, BigDecimal income, String rate) {
        BigDecimal limit = income.multiply(new BigDecimal(rate)).setScale(2, RoundingMode.HALF_UP);
        return new PlannedCategory(name, type, limit);
    }
}
