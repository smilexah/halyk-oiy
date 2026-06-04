package kz.halyk.maqsat.summary.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import kz.halyk.maqsat.summary.client.ActivePlanView;
import kz.halyk.maqsat.summary.client.OpenAiClient;
import kz.halyk.maqsat.summary.client.UserMetricsDto;
import kz.halyk.maqsat.summary.config.OpenAiProperties;
import kz.halyk.maqsat.summary.dto.res.SummaryResult;
import kz.halyk.maqsat.summary.exceptions.AiResponseException;
import kz.halyk.maqsat.summary.services.SummaryLlmService;
import org.springframework.stereotype.Service;

@Service
public class SummaryLlmServiceImpl implements SummaryLlmService {

    private final OpenAiProperties props;
    private final OpenAiClient openAi;
    private final ObjectMapper mapper;
    private final MeterRegistry meterRegistry;

    public SummaryLlmServiceImpl(OpenAiProperties props, OpenAiClient openAi,
                                  ObjectMapper mapper, MeterRegistry meterRegistry) {
        this.props = props;
        this.openAi = openAi;
        this.mapper = mapper;
        this.meterRegistry = meterRegistry;
    }

    private static final Map<String, String> SYSTEM_BY_LANG = Map.of(
            "ru", """
                    You are an in-house financial summary LLM. Tone: friendly, concise, Halyk Bank Kazakhstan.
                    Output STRICT JSON {"language":"ru","summaryText":"...","highlights":["..."],"suggestions":["..."]}.
                    Language: Russian. 2–4 highlights, 1–3 suggestions. No markdown.
                    """,
            "kk", """
                    You are an in-house financial summary LLM. Tone: friendly, concise, Halyk Bank Kazakhstan.
                    Output STRICT JSON {"language":"kk","summaryText":"...","highlights":["..."],"suggestions":["..."]}.
                    Language: Kazakh. 2–4 highlights, 1–3 suggestions. No markdown.
                    """,
            "en", """
                    You are an in-house financial summary LLM. Tone: friendly, concise, Halyk Bank Kazakhstan.
                    Output STRICT JSON {"language":"en","summaryText":"...","highlights":["..."],"suggestions":["..."]}.
                    Language: English. 2–4 highlights, 1–3 suggestions. No markdown.
                    """);

    @Override
    public SummaryResult summarise(String userId, String period, String language,
                                   UserMetricsDto metrics, ActivePlanView plan) {
        if (!props.enabled()) {
            meterRegistry.counter("maqsat.openai.calls", "service", "summary-llm", "outcome", "fallback").increment();
            SummaryResult r = fallback(period, language, metrics);
            meterRegistry.counter("maqsat.summaries.generated", "language", r.language()).increment();
            return r;
        }
        String system = SYSTEM_BY_LANG.getOrDefault(language, SYSTEM_BY_LANG.get("ru"));
        String userPrompt = buildPrompt(userId, period, metrics, plan);
        try {
            String raw = openAi.complete(system, List.of(new OpenAiClient.ChatMessage("user", userPrompt)));
            SummaryResult r = mapper.readValue(raw, SummaryResult.class);
            meterRegistry.counter("maqsat.openai.calls", "service", "summary-llm", "outcome", "ok").increment();
            meterRegistry.counter("maqsat.summaries.generated", "language", r.language()).increment();
            return r;
        } catch (Exception e) {
            meterRegistry.counter("maqsat.openai.calls", "service", "summary-llm", "outcome", "error").increment();
            throw new AiResponseException("Bad OpenAI JSON", e);
        }
    }

    private SummaryResult fallback(String period, String language, UserMetricsDto m) {
        BigDecimal spent = m == null ? BigDecimal.ZERO : m.totalSpent();
        String text = "За " + period + " потрачено " + spent + " ₸. (fallback, OPENAI_API_KEY не задан)";
        return new SummaryResult(language, text, List.of("totalSpent=" + spent),
                List.of("Установите OPENAI_API_KEY для умных рекомендаций"));
    }

    private String buildPrompt(String userId, String period, UserMetricsDto m, ActivePlanView p) {
        try {
            return "user_id: " + userId + "\nperiod: " + period
                    + "\nmetrics: " + mapper.writeValueAsString(m)
                    + "\nactive_plan: " + mapper.writeValueAsString(p)
                    + "\nProduce a short spending summary with highlights and 1-3 actionable suggestions.";
        } catch (Exception e) {
            throw new AiResponseException("Could not build prompt", e);
        }
    }
}
