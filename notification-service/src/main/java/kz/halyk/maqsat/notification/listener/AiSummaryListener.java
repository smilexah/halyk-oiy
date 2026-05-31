package kz.halyk.maqsat.notification.listener;

import kz.halyk.maqsat.common.event.EventTopics;
import kz.halyk.maqsat.common.event.SummaryGenerated;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumes AI summary events and logs them as Smart-Push notifications. */
@Component
@Slf4j
public class AiSummaryListener {

    @KafkaListener(topics = EventTopics.AI_SUMMARY_GENERATED, groupId = "notification-service")
    public void onSummary(SummaryGenerated e) {
        log.info("🔔 AI-SUMMARY → user {} ({}, period {}): {}",
                e.userId(), e.language(), e.period(), e.summaryText());
        if (e.highlights() != null && !e.highlights().isEmpty()) {
            log.info("   highlights: {}", e.highlights());
        }
        if (e.suggestions() != null && !e.suggestions().isEmpty()) {
            log.info("   suggestions: {}", e.suggestions());
        }
    }
}
