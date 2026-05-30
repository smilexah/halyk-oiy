package kz.halyk.maqsat.notification.listener;

import kz.halyk.maqsat.common.event.EventTopics;
import kz.halyk.maqsat.common.event.LimitExceeded;
import kz.halyk.maqsat.common.event.LimitOverrideApproved;
import kz.halyk.maqsat.common.event.TransactionCategorized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Imitates Smart-Push: turns domain events into (logged) push notifications. */
@Component
@Slf4j
public class SmartPushListener {

    @KafkaListener(topics = EventTopics.TRANSACTION_LIMIT_EXCEEDED, groupId = "notification-service")
    public void onLimitExceeded(LimitExceeded event) {
        log.warn("🔔 SOS-PUSH → parent: child {} reached the daily limit at '{}' (need {} more, limit {}). "
                        + "Tap to approve transaction {}.",
                event.childUserId(), event.merchant(), event.shortfall(), event.dailyLimit(), event.transactionId());
    }

    @KafkaListener(topics = EventTopics.FAMILY_LIMIT_OVERRIDE_APPROVED, groupId = "notification-service")
    public void onOverrideApproved(LimitOverrideApproved event) {
        log.info("🔔 PUSH → child {}: your parent approved {}. Payment for transaction {} is going through.",
                event.childUserId(), event.approvedAmount(), event.transactionId());
    }

    @KafkaListener(topics = EventTopics.TRANSACTION_CATEGORIZED, groupId = "notification-service")
    public void onCategorized(TransactionCategorized event) {
        log.info("🔔 PUSH → user {}: spent {} on '{}'. Tracked against your budget.",
                event.userId(), event.amount(), event.categoryName());
    }
}
