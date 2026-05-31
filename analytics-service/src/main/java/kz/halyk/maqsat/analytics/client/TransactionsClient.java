package kz.halyk.maqsat.analytics.client;

import java.time.Instant;
import java.util.List;
import kz.halyk.maqsat.common.event.TransactionCategorized;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * HTTP client for the transaction-service internal backfill endpoint.
 * Uses Eureka-backed load-balancing via the {@code lb://} scheme.
 */
@Component
public class TransactionsClient {

    private final WebClient web;

    public TransactionsClient(@LoadBalanced WebClient.Builder builder) {
        this.web = builder.baseUrl("lb://transaction-service").build();
    }

    /**
     * Fetches all transactions with occurredAt >= since from transaction-service
     * and maps them to {@link TransactionCategorized} events.
     *
     * @param since lower bound (inclusive) for occurredAt
     * @return list of events; empty list if the remote call returns nothing
     */
    public List<TransactionCategorized> fetchSince(Instant since) {
        return web.get()
                .uri(uri -> uri.path("/api/transactions/internal/since")
                        .queryParam("since", since.toString())
                        .build())
                .retrieve()
                .bodyToFlux(TransactionResponseDto.class)
                .map(TransactionResponseDto::toEvent)
                .collectList()
                .block();
    }
}
