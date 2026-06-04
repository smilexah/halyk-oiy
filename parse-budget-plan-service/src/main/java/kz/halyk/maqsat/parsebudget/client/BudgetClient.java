package kz.halyk.maqsat.parsebudget.client;

import java.util.Optional;
import java.util.UUID;
import kz.halyk.maqsat.parsebudget.dto.req.ParsedPlanPayload;
import kz.halyk.maqsat.parsebudget.dto.res.ActivePlanView;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class BudgetClient {

    private final WebClient web;

    public BudgetClient(@LoadBalanced WebClient.Builder builder) {
        this.web = builder.baseUrl("lb://budget-service").build();
    }

    public Optional<ActivePlanView> getActive(String userId) {
        try {
            ActivePlanView v = web.get()
                    .uri("/api/budget/internal/active/{u}", userId)
                    .retrieve()
                    .bodyToMono(ActivePlanView.class)
                    .block();
            return Optional.ofNullable(v);
        } catch (WebClientResponseException.NotFound e) {
            return Optional.empty();
        }
    }

    public UUID replan(String userId, ParsedPlanPayload payload) {
        return web.post()
                .uri("/api/budget/internal/replan/{u}", userId)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(UUID.class)
                .block();
    }
}
