package kz.halyk.maqsat.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import kz.halyk.maqsat.ai.config.AnthropicProperties;
import kz.halyk.maqsat.ai.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Minimal Anthropic Messages API client. Returns the assistant's text content. */
@Component
@RequiredArgsConstructor
public class AnthropicClient {

    private final WebClient anthropicWebClient;
    private final AnthropicProperties properties;

    public String complete(String system, List<ChatMessage> conversation) {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (ChatMessage m : conversation) {
            messages.add(Map.of("role", m.role(), "content", m.content()));
        }

        Map<String, Object> body = Map.of(
                "model", properties.model(),
                "max_tokens", properties.maxTokens(),
                "system", system,
                "messages", messages);

        JsonNode response = anthropicWebClient.post()
                .uri("/v1/messages")
                .header("x-api-key", properties.apiKey())
                .header("anthropic-version", properties.version())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null) {
            throw new IllegalStateException("Empty response from Anthropic");
        }
        return response.path("content").path(0).path("text").asText();
    }
}
