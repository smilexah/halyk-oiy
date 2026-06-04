package kz.halyk.maqsat.recommendation.listeners;

import kz.halyk.maqsat.common.event.EventTopics;
import kz.halyk.maqsat.common.event.MetricsComputed;
import kz.halyk.maqsat.common.event.RecommendationReady;
import kz.halyk.maqsat.recommendation.dto.res.RecommendationResult;
import kz.halyk.maqsat.recommendation.services.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsComputedListener {

    private final RecommendationService svc;
    private final KafkaTemplate<String, Object> producer;

    @KafkaListener(topics = EventTopics.ANALYTICS_METRICS_COMPUTED, groupId = "recommendation-service")
    public void onMetrics(MetricsComputed e) {
        log.info("Received MetricsComputed for userId={} period={}", e.userId(), e.period());
        RecommendationResult r = svc.recommend(e.userId(), e.period());
        producer.send(EventTopics.AI_RECOMMENDATION_READY, e.userId(),
                new RecommendationReady(e.userId(), e.period(),
                        r.offers().stream()
                                .map(m -> new RecommendationReady.MatchedOffer(
                                        m.offerId(), m.partner(), m.score(), m.audienceTags(), m.rationale()))
                                .toList(),
                        r.generatedAt()));
        log.info("Published RecommendationReady for userId={} period={}", e.userId(), e.period());
    }
}
