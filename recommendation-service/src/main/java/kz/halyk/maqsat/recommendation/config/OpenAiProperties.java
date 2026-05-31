package kz.halyk.maqsat.recommendation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
        String apiKey,
        String baseUrl,
        String model,
        Integer maxTokens,
        Double temperature
) {
    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
