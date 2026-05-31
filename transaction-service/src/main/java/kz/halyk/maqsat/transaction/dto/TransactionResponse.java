package kz.halyk.maqsat.transaction.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import kz.halyk.maqsat.transaction.domain.Transaction;

public record TransactionResponse(
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
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getAccountId(),
                t.getUserId(),
                t.getAmount(),
                t.getMerchant(),
                t.getMcc(),
                t.getCategoryName(),
                t.getOccurredAt(),
                t.getStatus() != null ? t.getStatus().name() : null,
                t.getDirection() != null ? t.getDirection().name() : null,
                t.getOperationType() != null ? t.getOperationType().name() : null,
                t.getCurrency(),
                t.getDetails(),
                t.getBalanceAfter()
        );
    }
}