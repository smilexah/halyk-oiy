package kz.halyk.maqsat.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

public record TransactionRequest(
        @NotBlank String accountId,
        @NotNull @Positive BigDecimal amount,
        String merchant,
        String mcc,
        Instant occurredAt
) {
}