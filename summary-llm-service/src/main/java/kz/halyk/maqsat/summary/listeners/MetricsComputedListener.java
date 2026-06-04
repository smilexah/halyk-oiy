package kz.halyk.maqsat.summary.listeners;

import kz.halyk.maqsat.common.event.EventTopics;
import kz.halyk.maqsat.common.event.MetricsComputed;
import kz.halyk.maqsat.summary.client.AnalyticsClient;
import kz.halyk.maqsat.summary.client.BudgetClient;
import kz.halyk.maqsat.summary.client.UserMetricsDto;
import kz.halyk.maqsat.summary.client.ActivePlanView;
import kz.halyk.maqsat.summary.dto.res.SummaryResult;
import kz.halyk.maqsat.summary.event.SummaryEventPublisher;
import kz.halyk.maqsat.summary.services.LanguagePicker;
import kz.halyk.maqsat.summary.services.SummaryLlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsComputedListener {

    private final AnalyticsClient analytics;
    private final BudgetClient budget;
    private final SummaryLlmService svc;
    private final LanguagePicker languagePicker;
    private final SummaryEventPublisher publisher;

    @KafkaListener(topics = EventTopics.ANALYTICS_METRICS_COMPUTED, groupId = "summary-llm-service")
    public void onMetrics(MetricsComputed e) {
        log.info("Received MetricsComputed for userId={} period={}", e.userId(), e.period());
        UserMetricsDto metrics = analytics.fetchMetrics(e.userId(), e.period());
        ActivePlanView plan = budget.getActive(e.userId());
        SummaryResult r = svc.summarise(e.userId(), e.period(), languagePicker.pick(e.userId()), metrics, plan);
        publisher.publish(e.userId(), e.period(), r);
        log.info("Published SummaryGenerated for userId={} period={}", e.userId(), e.period());
    }
}
