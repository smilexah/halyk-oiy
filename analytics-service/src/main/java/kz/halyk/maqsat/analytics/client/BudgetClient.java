package kz.halyk.maqsat.analytics.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * HTTP client for fetching the active budget plan from budget-service via Eureka.
 * Uses the internal endpoint {@code GET /api/budget/internal/active/{userId}} which
 * does not require a user JWT.
 */
@Component
@Slf4j
public class BudgetClient {

    /** Lightweight projection of a user's active budget plan. */
    public record ActivePlan(UUID planId, String period, List<CategoryLimit> categories) {}

    /** Per-category limit within a budget plan. */
    public record CategoryLimit(String name, BigDecimal limit, String type) {}

    private final WebClient web;

    public BudgetClient(@LoadBalanced WebClient.Builder builder) {
        this.web = builder.baseUrl("lb://budget-service").build();
    }

    /**
     * Fetches the active budget plan for the given user.
     *
     * @param userId the user identifier (JWT sub)
     * @return the active plan, or {@code null} if budget-service returns 404
     */
    public ActivePlan fetchActivePlan(String userId) {
        return web.get()
                .uri("/api/budget/internal/active/{userId}", userId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp -> {
                    if (resp.statusCode().value() == 404) {
                        return Mono.empty();
                    }
                    return resp.createException().flatMap(Mono::error);
                })
                .bodyToMono(ActivePlan.class)
                .doOnError(ex -> log.warn("Failed to fetch active plan for userId={}: {}", userId, ex.getMessage()))
                .onErrorReturn(null)
                .block();
    }
}
