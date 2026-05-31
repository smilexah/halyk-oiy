package kz.halyk.maqsat.financial.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import kz.halyk.maqsat.financial.client.OpenAiClient;
import kz.halyk.maqsat.financial.config.OpenAiProperties;
import kz.halyk.maqsat.financial.dto.BudgetPlanProposal;
import kz.halyk.maqsat.financial.dto.PlannedCategory;
import kz.halyk.maqsat.financial.exception.AiResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialAgentService {

    private final OpenAiProperties props;
    private final OpenAiClient openAi;
    private final PriorsService priorsSvc;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String SYSTEM = """
            You are an in-house personal-finance ML model emulated as an LLM.
            Output STRICT JSON {"categories":[{"name":...,"type":"MANDATORY|DISCRETIONARY","limitAmount":number}],"rationale":"..."}.
            Use only the Russian category names supplied in the user prompt.
            Return BigDecimal-safe integers/decimals only. Never exceed monthly income.
            """;

    public BudgetPlanProposal propose(String userId, String period, String segmentTag,
                                      Map<String, BigDecimal> metrics,
                                      Map<String, BigDecimal> driftByCategory) {
        Map<String, BigDecimal> priors = priorsSvc.getPriorsFor(segmentTag);
        if (!props.enabled()) {
            log.info("OPENAI_API_KEY not set — using priors-baseline fallback for user={}", userId);
            return fallback(metrics, priors);
        }
        String userPrompt = buildPrompt(userId, period, metrics, priors, driftByCategory);
        String raw = openAi.complete(SYSTEM, List.of(new OpenAiClient.ChatMessage("user", userPrompt)));
        return parse(raw);
    }

    private BudgetPlanProposal fallback(Map<String, BigDecimal> metrics, Map<String, BigDecimal> priors) {
        BigDecimal income = metrics.getOrDefault("incomeEstimate", BigDecimal.ZERO);
        List<PlannedCategory> cats = priors.entrySet().stream()
                .map(e -> new PlannedCategory(
                        e.getKey(),
                        List.of("Продукты", "Коммуналка", "Транспорт").contains(e.getKey()) ? "MANDATORY" : "DISCRETIONARY",
                        income.multiply(e.getValue()).setScale(0, RoundingMode.HALF_UP)))
                .toList();
        return new BudgetPlanProposal(cats, "fallback: priors-baseline (no OPENAI_API_KEY)");
    }

    private BudgetPlanProposal parse(String raw) {
        try {
            return mapper.readValue(raw, BudgetPlanProposal.class);
        } catch (Exception e) {
            throw new AiResponseException("Bad OpenAI JSON: " + raw, e);
        }
    }

    private String buildPrompt(String userId, String period,
                                Map<String, BigDecimal> metrics,
                                Map<String, BigDecimal> priors,
                                Map<String, BigDecimal> drift) {
        try {
            return """
                    user_id: %s
                    period: %s
                    metrics: %s
                    population_priors_for_segment: %s
                    drift_to_correct_by_category: %s
                    Produce a re-planned budget that respects priors, reacts to drift, and stays under income.
                    """.formatted(userId, period,
                    mapper.writeValueAsString(metrics),
                    mapper.writeValueAsString(priors),
                    mapper.writeValueAsString(drift));
        } catch (Exception e) {
            throw new AiResponseException("Could not build prompt", e);
        }
    }
}
