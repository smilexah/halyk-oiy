package kz.halyk.maqsat.transaction.event;

import java.math.BigDecimal;
import kz.halyk.maqsat.common.event.EventTopics;
import kz.halyk.maqsat.common.event.LimitExceeded;
import kz.halyk.maqsat.common.event.TransactionCategorized;
import kz.halyk.maqsat.transaction.domain.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCategorized(Transaction t) {
        TransactionCategorized event = new TransactionCategorized(
                t.getId(),
                t.getUserId(),
                t.getAccountId(),
                t.getAmount(),
                t.getMcc(),
                t.getCategoryName(),
                t.getOccurredAt());
        kafkaTemplate.send(EventTopics.TRANSACTION_CATEGORIZED, t.getUserId(), event);
    }

    public void publishLimitExceeded(Transaction t, BigDecimal dailyLimit, BigDecimal shortfall) {
        LimitExceeded event = new LimitExceeded(
                t.getId(),
                t.getUserId(),
                t.getAccountId(),
                t.getAmount(),
                dailyLimit,
                shortfall,
                t.getMcc(),
                t.getMerchant());
        kafkaTemplate.send(EventTopics.TRANSACTION_LIMIT_EXCEEDED, t.getUserId(), event);
    }
}
