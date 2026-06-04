package kz.halyk.maqsat.integration.listeners;

import kz.halyk.maqsat.common.event.EventTopics;
import kz.halyk.maqsat.common.event.RecommendationReady;
import kz.halyk.maqsat.integration.client.AlserClient;
import kz.halyk.maqsat.integration.client.HalykTravelClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendationListener {

    private final AlserClient alser;
    private final HalykTravelClient halykTravel;

    @KafkaListener(topics = EventTopics.AI_RECOMMENDATION_READY, groupId = "integration-service")
    public void onRecommendation(RecommendationReady e) {
        for (var match : e.offers()) {
            String result = switch (match.partner()) {
                case "ALSER" -> alser.notifyTargeting(e.userId(), match.offerId(), match.score());
                case "HALYK_TRAVEL" -> halykTravel.notifyTargeting(e.userId(), match.offerId(), match.score());
                default -> "unknown-partner";
            };
            log.info("PARTNER {}: user={} offer={} score={} audience={} result={}",
                    match.partner(), e.userId(), match.offerId(), match.score(), match.audienceTags(), result);
        }
    }
}
