package kz.halyk.maqsat.recommendation.client;

import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** Calls goals-service via Eureka load-balancer to fetch user goals. */
@Component
@Slf4j
public class GoalsClient {

    private final WebClient webClient;

    public GoalsClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClient = loadBalancedWebClientBuilder
                .baseUrl("lb://goals-service")
                .build();
    }

    /**
     * Returns the list of goals for the given user, or an empty list if not found (404).
     */
    public List<GoalDto> fetchGoals(String userId) {
        try {
            List<GoalDto> result = webClient.get()
                    .uri("/api/goals/internal/{userId}", userId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<GoalDto>>() {})
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (WebClientResponseException.NotFound e) {
            log.warn("No goals found for userId={}", userId);
            return Collections.emptyList();
        }
    }
}
