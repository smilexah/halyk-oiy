package kz.halyk.maqsat.transaction.dto.res;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
) {}
