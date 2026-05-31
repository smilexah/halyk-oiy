package kz.halyk.maqsat.recommendation.client;

import java.math.BigDecimal;
import java.util.List;
import kz.halyk.maqsat.recommendation.dto.MatchedOffer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Calls analytics-service internal write endpoint to persist a recommendation batch. */
@Component
@Slf4j
public class AnalyticsWriteClient {

    private final WebClient webClient;

    public AnalyticsWriteClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClient = loadBalancedWebClientBuilder
                .baseUrl("lb://analytics-service")
                .build();
    }

    public void write(String userId, String period, List<MatchedOffer> offers) {
        List<BatchItem> items = offers.stream()
                .map(o -> new BatchItem(o.offerId(), o.partner(), o.score(), o.audienceTags(), o.rationale()))
                .toList();
        BatchPayload payload = new BatchPayload(userId, period, items);
        try {
            webClient.post()
                    .uri("/api/analytics/internal/recommendations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Persisted {} recommendations for userId={} period={}", items.size(), userId, period);
        } catch (Exception e) {
            log.error("Failed to persist recommendations for userId={} period={}: {}", userId, period, e.getMessage());
        }
    }

    private record BatchPayload(String userId, String period, List<BatchItem> items) {}

    private record BatchItem(
            String offerId,
            String partner,
            BigDecimal score,
            List<String> audienceTags,
            String rationale
    ) {}
}
