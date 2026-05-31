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

/** Calls halyk-travel-mock-service via Eureka load-balancer to fetch travel offers filtered by audience. */
@Component
@Slf4j
public class HalykTravelClient {

    private final WebClient webClient;

    public HalykTravelClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClient = loadBalancedWebClientBuilder
                .baseUrl("lb://halyk-travel-mock-service")
                .build();
    }

    /**
     * Returns travel offers matching the given audience tags, or an empty list if not found (404).
     */
    public List<TravelOfferDto> fetchOffers(Set<String> audienceTags) {
        try {
            List<TravelOfferDto> result = webClient.get()
                    .uri(uriBuilder -> {
                        UriBuilder builder = uriBuilder.path("/api/halyk-travel/offers");
                        for (String tag : audienceTags) {
                            builder = builder.queryParam("audience", tag);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<TravelOfferDto>>() {})
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (WebClientResponseException.NotFound e) {
            log.warn("No travel offers found for tags={}", audienceTags);
            return Collections.emptyList();
        }
    }
}
