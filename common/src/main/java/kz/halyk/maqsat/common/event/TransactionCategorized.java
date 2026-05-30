package kz.halyk.maqsat.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Published on topic {@link EventTopics#TRANSACTION_CATEGORIZED} after a transaction is categorized. */
public record TransactionCategorized(
        UUID transactionId,
        String userId,
        String accountId,
        BigDecimal amount,
        String mcc,
        String categoryName,
        Instant occurredAt
) {
}