package kz.halyk.maqsat.integration.client;

import java.math.BigDecimal;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Slf4j
public class HalykTravelClient {

    private final WebClient webClient;

    public HalykTravelClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClient = loadBalancedWebClientBuilder
                .baseUrl("lb://halyk-travel-mock-service")
                .build();
    }

    public String notifyTargeting(String userId, String offerId, BigDecimal score) {
        try {
            String name = webClient.get()
                    .uri("/api/halyk-travel/offers/{id}", offerId)
                    .retrieve()
                    .bodyToMono(com.fasterxml.jackson.databind.JsonNode.class)
                    .map(node -> node.path("destination").asText(
                            node.path("name").asText("unknown")))
                    .block(Duration.ofSeconds(2));
            return "halyk-travel-ok offer=" + name;
        } catch (Exception e) {
            log.warn("HalykTravelClient unreachable for offerId={}: {}", offerId, e.getMessage());
            return "halyk-travel-unreachable";
        }
    }
}
