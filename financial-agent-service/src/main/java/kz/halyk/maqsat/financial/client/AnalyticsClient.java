package kz.halyk.maqsat.financial.client;

import kz.halyk.maqsat.financial.dto.res.UserMetricsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AnalyticsClient {

    private final WebClient web;

    public AnalyticsClient(@LoadBalanced WebClient.Builder builder) {
        this.web = builder.baseUrl("lb://analytics-service").build();
    }

    public UserMetricsDto fetchMetrics(String userId, String period) {
        return web.get()
                .uri("/api/analytics/metrics/{u}/{p}", userId, period)
                .retrieve()
                .bodyToMono(UserMetricsDto.class)
                .onErrorResume(e -> {
                    log.warn("Failed to fetch metrics for user={} period={}: {}", userId, period, e.getMessage());
                    return Mono.empty();
                })
                .block();
    }
}
