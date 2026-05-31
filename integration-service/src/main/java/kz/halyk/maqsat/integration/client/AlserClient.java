package kz.halyk.maqsat.integration.client;

import java.math.BigDecimal;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Slf4j
public class AlserClient {

    private final WebClient webClient;

    public AlserClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClient = loadBalancedWebClientBuilder
                .baseUrl("lb://alser-mock-service")
                .build();
    }

    public String notifyTargeting(String userId, String offerId, BigDecimal score) {
        try {
            String name = webClient.get()
                    .uri("/api/alser/offers/{id}", offerId)
                    .retrieve()
                    .bodyToMono(com.fasterxml.jackson.databind.JsonNode.class)
                    .map(node -> node.path("name").asText("unknown"))
                    .block(Duration.ofSeconds(2));
            return "alser-ok offer=" + name;
        } catch (Exception e) {
            log.warn("AlserClient unreachable for offerId={}: {}", offerId, e.getMessage());
            return "alser-unreachable";
        }
    }
}
