package kz.halyk.maqsat.recommendation.client;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;

/** Calls alser-mock-service via Eureka load-balancer to fetch device offers filtered by audience. */
@Component
@Slf4j
public class AlserClient {

    private final WebClient webClient;

    public AlserClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClient = loadBalancedWebClientBuilder
                .baseUrl("lb://alser-mock-service")
                .build();
    }

    /**
     * Returns device offers matching the given audience tags, or an empty list if not found (404).
     */
    public List<DeviceOfferDto> fetchOffers(Set<String> audienceTags) {
        try {
            List<DeviceOfferDto> result = webClient.get()
                    .uri(uriBuilder -> {
                        UriBuilder builder = uriBuilder.path("/api/alser/offers");
                        for (String tag : audienceTags) {
                            builder = builder.queryParam("audience", tag);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<DeviceOfferDto>>() {})
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (WebClientResponseException.NotFound e) {
            log.warn("No alser offers found for tags={}", audienceTags);
            return Collections.emptyList();
        }
    }
}
