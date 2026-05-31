package kz.halyk.maqsat.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @deprecated Anthropic integration replaced by OpenAI in the new AI plane services.
 */
@Deprecated
@ConfigurationProperties(prefix = "anthropic")
public record AnthropicProperties(
        String apiKey,
        String baseUrl,
        String model,
        String version,
        Integer maxTokens
) {
    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
