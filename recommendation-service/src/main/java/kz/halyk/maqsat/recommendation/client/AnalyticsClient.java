package kz.halyk.maqsat.recommendation.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** Calls analytics-service via Eureka load-balancer to fetch computed metrics. */
@Component
@Slf4j
public class AnalyticsClient {

    private final WebClient webClient;

    public AnalyticsClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClient = loadBalancedWebClientBuilder
                .baseUrl("lb://analytics-service")
                .build();
    }

    /**
     * Returns metrics for the given user and period, or {@code null} if not found (404).
     */
    public UserMetricsDto fetchMetrics(String userId, String period) {
        try {
            return webClient.get()
                    .uri("/api/analytics/metrics/{userId}/{period}", userId, period)
                    .retrieve()
                    .bodyToMono(UserMetricsDto.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            log.warn("No metrics found for userId={} period={}", userId, period);
            return null;
        }
    }
}
