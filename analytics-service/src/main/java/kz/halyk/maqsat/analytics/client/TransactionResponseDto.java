package kz.halyk.maqsat.analytics.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import kz.halyk.maqsat.common.event.TransactionCategorized;

/**
 * Local DTO mirroring transaction-service's TransactionResponse.
 * Used by {@link TransactionsClient} to deserialize the /internal/since response
 * and convert it into {@link TransactionCategorized} events for the ingest pipeline.
 */
public record TransactionResponseDto(
        UUID id,
        String accountId,
        String userId,
        BigDecimal amount,
        String merchant,
        String mcc,
        String categoryName,
        Instant occurredAt,
        String status,
        String direction,
        String operationType,
        String currency,
        String details,
        BigDecimal balanceAfter
) {
    /** Converts to a {@link TransactionCategorized} event for the ingest pipeline. */
    public TransactionCategorized toEvent() {
        return new TransactionCategorized(
                id,
                userId,
                accountId,
                amount,
                mcc,
                categoryName != null ? categoryName : "Прочее",
                occurredAt,
                direction,
                operationType,
                currency != null ? currency : "KZT",
                details,
                balanceAfter
        );
    }
}
