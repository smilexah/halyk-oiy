package kz.halyk.maqsat.common.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Published on topic {@link EventTopics#TRANSACTION_LIMIT_EXCEEDED} when a child exceeds the daily limit. */
public record LimitExceeded(
        UUID transactionId,
        String childUserId,
        String accountId,
        BigDecimal attemptedAmount,
        BigDecimal dailyLimit,
        BigDecimal shortfall,
        String mcc,
        String merchant
) {
}