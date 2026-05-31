package kz.halyk.maqsat.analytics.listener;

import kz.halyk.maqsat.analytics.service.IngestService;
import kz.halyk.maqsat.common.event.EventTopics;
import kz.halyk.maqsat.common.event.TransactionCategorized;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionCategorizedListener {

    private final IngestService ingest;

    @KafkaListener(topics = EventTopics.TRANSACTION_CATEGORIZED, groupId = "analytics-service")
    public void onMessage(TransactionCategorized e) {
        ingest.handle(e);
    }
}
