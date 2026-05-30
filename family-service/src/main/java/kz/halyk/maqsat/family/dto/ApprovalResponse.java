package kz.halyk.maqsat.family.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ApprovalResponse(
        UUID transactionId,
        String childUserId,
        BigDecimal approvedAmount,
        String approvedByUserId,
        Instant overrideUntil
) {
}
