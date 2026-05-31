package kz.halyk.maqsat.integration.listener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import kz.halyk.maqsat.common.event.RecommendationReady;
import kz.halyk.maqsat.integration.client.AlserClient;
import kz.halyk.maqsat.integration.client.HalykTravelClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationListenerTest {

    @Mock
    AlserClient alserClient;

    @Mock
    HalykTravelClient halykTravelClient;

    @InjectMocks
    RecommendationListener listener;

    @Test
    void routesAlserAndTravelOffersToCorrectClients() {
        String userId = "user-1";
        String alserOfferId = "alser-offer-123";
        String travelOfferId = "travel-offer-456";

        when(alserClient.notifyTargeting(userId, alserOfferId, new BigDecimal("0.850")))
                .thenReturn("alser-ok offer=iPhone 15 Pro");
        when(halykTravelClient.notifyTargeting(userId, travelOfferId, new BigDecimal("0.720")))
                .thenReturn("halyk-travel-ok offer=Astana → Antalya");

        RecommendationReady event = new RecommendationReady(
                userId,
                "2026-05",
                List.of(
                        new RecommendationReady.MatchedOffer(
                                alserOfferId, "ALSER",
                                new BigDecimal("0.850"),
                                List.of("electronics-saver", "apple-fan"),
                                "high overlap"),
                        new RecommendationReady.MatchedOffer(
                                travelOfferId, "HALYK_TRAVEL",
                                new BigDecimal("0.720"),
                                List.of("saving_for_trip", "beach-lover"),
                                "saving goal matched")
                ),
                Instant.now()
        );

        listener.onRecommendation(event);

        verify(alserClient).notifyTargeting(userId, alserOfferId, new BigDecimal("0.850"));
        verify(halykTravelClient).notifyTargeting(userId, travelOfferId, new BigDecimal("0.720"));
    }
}
