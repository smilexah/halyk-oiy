package kz.halyk.maqsat.financial.client;

import kz.halyk.maqsat.financial.dto.ReplanRequest;
import kz.halyk.maqsat.financial.dto.ReplanResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Slf4j
public class ParseBudgetPlanClient {

    private final WebClient web;

    public ParseBudgetPlanClient(@LoadBalanced WebClient.Builder builder) {
        this.web = builder.baseUrl("lb://parse-budget-plan-service").build();
    }

    public ReplanResponse replan(ReplanRequest req) {
        return web.post()
                .uri("/api/parse-budget/replan")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(ReplanResponse.class)
                .onErrorResume(e -> {
                    log.warn("Failed to send replan request for user={}: {}", req.userId(), e.getMessage());
                    return reactor.core.publisher.Mono.empty();
                })
                .block();
    }
}
