package kz.halyk.maqsat.family.event;

import java.math.BigDecimal;
import java.util.UUID;
import kz.halyk.maqsat.common.event.EventTopics;
import kz.halyk.maqsat.common.event.LimitOverrideApproved;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FamilyEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOverrideApproved(UUID transactionId, String childUserId, BigDecimal approvedAmount, String approvedByUserId) {
        LimitOverrideApproved event = new LimitOverrideApproved(transactionId, childUserId, approvedAmount, approvedByUserId);
        kafkaTemplate.send(EventTopics.FAMILY_LIMIT_OVERRIDE_APPROVED, childUserId, event);
    }
}
