package kz.halyk.maqsat.recommendation.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import kz.halyk.maqsat.recommendation.config.OpenAiProperties;
import kz.halyk.maqsat.recommendation.exceptions.AiResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Minimal OpenAI Chat Completions API client with JSON mode. */
@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final WebClient openAiWebClient;
    private final OpenAiProperties props;

    public String complete(String system, List<ChatMessage> conversation) {
        if (!props.enabled()) {
            throw new IllegalStateException("OPENAI_API_KEY not set");
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", system));
        for (var m : conversation) {
            messages.add(Map.of("role", m.role(), "content", m.content()));
        }

        Map<String, Object> body = Map.of(
                "model", props.model(),
                "max_tokens", props.maxTokens(),
                "temperature", props.temperature(),
                "messages", messages,
                "response_format", Map.of("type", "json_object"));

        JsonNode resp = openAiWebClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + props.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (resp == null) {
            throw new AiResponseException("Empty response from OpenAI");
        }
        return resp.path("choices").path(0).path("message").path("content").asText();
    }

    public record ChatMessage(String role, String content) {}
}
