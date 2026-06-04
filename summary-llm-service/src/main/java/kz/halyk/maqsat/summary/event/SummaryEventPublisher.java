package kz.halyk.maqsat.summary.event;

import java.time.Instant;
import kz.halyk.maqsat.common.event.EventTopics;
import kz.halyk.maqsat.common.event.SummaryGenerated;
import kz.halyk.maqsat.summary.dto.res.SummaryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SummaryEventPublisher {

    private final KafkaTemplate<String, Object> producer;

    public void publish(String userId, String period, SummaryResult r) {
        producer.send(EventTopics.AI_SUMMARY_GENERATED, userId,
                new SummaryGenerated(userId, period, r.language(), r.summaryText(),
                        r.highlights(), r.suggestions(), Instant.now()));
    }
}
