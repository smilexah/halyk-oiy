package kz.halyk.maqsat.auth.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /** Load-balanced builder so we can target services by name (lb://family-service). */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    /** Talks to family-service through Eureka. */
    @Bean
    public WebClient familyWebClient(@LoadBalanced WebClient.Builder builder) {
        return builder.baseUrl("lb://family-service").build();
    }

    /** Talks to Keycloak at its absolute URL (not service-discovered). */
    @Bean
    public WebClient keycloakWebClient(KeycloakAdminProperties properties) {
        return WebClient.builder().baseUrl(properties.baseUrl()).build();
    }
}
