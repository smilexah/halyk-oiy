package kz.halyk.maqsat.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published on topic {@link EventTopics#TRANSACTION_CATEGORIZED} after a transaction is categorized.
 *
 * <p>The {@code direction} and {@code operationType} fields carry the string name of the
 * corresponding enum values ({@code Direction} / {@code OperationType} in transaction-service) so
 * that consumers do not need to depend on transaction-service-internal enums. Possible values:
 * <ul>
 *   <li>{@code direction}: {@code DEBIT}, {@code CREDIT}</li>
 *   <li>{@code operationType}: {@code PURCHASE}, {@code TRANSFER}, {@code TOPUP},
 *       {@code PAYMENT}, {@code SALARY}, {@code MISC}</li>
 * </ul>
 */
public record TransactionCategorized(
        UUID transactionId,
        String userId,
        String accountId,
        BigDecimal amount,
        String mcc,
        String categoryName,
        Instant occurredAt,
        String direction,
        String operationType,
        String currency,
        String details,
        BigDecimal balanceAfter
) {
}