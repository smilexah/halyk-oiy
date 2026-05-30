package kz.halyk.maqsat.integration.listener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import kz.halyk.maqsat.common.event.EventTopics;
import kz.halyk.maqsat.common.event.TransactionCategorized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Stub bonus/offer engine: reacts to spending with a cashback suggestion. */
@Component
@Slf4j
public class BonusListener {

    private static final BigDecimal CASHBACK_RATE = new BigDecimal("0.01");

    @KafkaListener(topics = EventTopics.TRANSACTION_CATEGORIZED, groupId = "integration-service")
    public void onCategorized(TransactionCategorized event) {
        BigDecimal cashback = event.amount().multiply(CASHBACK_RATE).setScale(2, RoundingMode.HALF_UP);
        log.info("🎁 OFFER → user {}: +{} bonus on '{}' purchase ({}).",
                event.userId(), cashback, event.categoryName(), event.amount());
    }
}
