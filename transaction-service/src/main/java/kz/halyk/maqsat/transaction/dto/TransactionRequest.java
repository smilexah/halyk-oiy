package kz.halyk.maqsat.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import kz.halyk.maqsat.transaction.domain.Direction;
import kz.halyk.maqsat.transaction.domain.OperationType;

/**
 * Inbound payload for creating a transaction.
 *
 * <p>The analytics-enrichment fields are all optional:
 * <ul>
 *   <li>{@code direction} — defaults to {@link Direction#DEBIT} when absent.</li>
 *   <li>{@code operationType} — defaults to {@link OperationType#PURCHASE} when absent.</li>
 *   <li>{@code currency} — defaults to {@code "KZT"} when absent.</li>
 *   <li>{@code details} — defaults to {@code null}.</li>
 *   <li>{@code balanceAfter} — defaults to {@code null}.</li>
 * </ul>
 */
public record TransactionRequest(
        @NotBlank String accountId,
        @NotNull @Positive BigDecimal amount,
        String merchant,
        String mcc,
        Instant occurredAt,
        Direction direction,
        OperationType operationType,
        String currency,
        String details,
        BigDecimal balanceAfter
) {
}