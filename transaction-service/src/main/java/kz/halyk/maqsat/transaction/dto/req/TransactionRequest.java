package kz.halyk.maqsat.transaction.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import kz.halyk.maqsat.transaction.entities.enums.Direction;
import kz.halyk.maqsat.transaction.entities.enums.OperationType;

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
) {}
