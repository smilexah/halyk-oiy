package kz.halyk.maqsat.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
