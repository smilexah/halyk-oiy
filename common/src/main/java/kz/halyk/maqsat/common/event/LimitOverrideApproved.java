package kz.halyk.maqsat.common.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Published on topic {@link EventTopics#FAMILY_LIMIT_OVERRIDE_APPROVED} when a parent approves an override. */
public record LimitOverrideApproved(
        UUID transactionId,
        String childUserId,
        BigDecimal approvedAmount,
        String approvedByUserId
) {
}