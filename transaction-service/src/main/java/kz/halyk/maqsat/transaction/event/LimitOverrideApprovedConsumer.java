package kz.halyk.maqsat.transaction.event;

import kz.halyk.maqsat.common.event.EventTopics;
import kz.halyk.maqsat.common.event.LimitOverrideApproved;
import kz.halyk.maqsat.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LimitOverrideApprovedConsumer {

    private final TransactionService transactionService;

    @KafkaListener(topics = EventTopics.FAMILY_LIMIT_OVERRIDE_APPROVED, groupId = "transaction-service")
    public void onLimitOverrideApproved(LimitOverrideApproved event) {
        log.info("Received LimitOverrideApproved for txn {} approvedBy {}", event.transactionId(), event.approvedByUserId());
        transactionService.approve(event.transactionId());
    }
}
