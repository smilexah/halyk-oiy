package kz.halyk.maqsat.summary.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** Calls budget-service via Eureka load-balancer to fetch the user's active budget plan. */
@Component
@Slf4j
public class BudgetClient {

    private final WebClient webClient;

    public BudgetClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClient = loadBalancedWebClientBuilder
                .baseUrl("lb://budget-service")
                .build();
    }

    /**
     * Returns the active plan for the given user, or {@code null} if not found (404).
     */
    public ActivePlanView getActive(String userId) {
        try {
            return webClient.get()
                    .uri("/api/budget/internal/active/{userId}", userId)
                    .retrieve()
                    .bodyToMono(ActivePlanView.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            log.warn("No active budget plan found for userId={}", userId);
            return null;
        }
    }
}
