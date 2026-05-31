package kz.halyk.maqsat.recommendation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kz.halyk.maqsat.recommendation.client.AnalyticsClient;
import kz.halyk.maqsat.recommendation.client.AnalyticsWriteClient;
import kz.halyk.maqsat.recommendation.client.AlserClient;
import kz.halyk.maqsat.recommendation.client.DeviceOfferDto;
import kz.halyk.maqsat.recommendation.client.GoalsClient;
import kz.halyk.maqsat.recommendation.client.HalykTravelClient;
import kz.halyk.maqsat.recommendation.client.OpenAiClient;
import kz.halyk.maqsat.recommendation.client.TravelOfferDto;
import kz.halyk.maqsat.recommendation.client.UserMetricsDto;
import kz.halyk.maqsat.recommendation.config.OpenAiProperties;
import kz.halyk.maqsat.recommendation.dto.MatchedOffer;
import kz.halyk.maqsat.recommendation.dto.RecommendationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock AnalyticsClient analyticsClient;
    @Mock GoalsClient goalsClient;
    @Mock AlserClient alserClient;
    @Mock HalykTravelClient travelClient;
    @Mock AnalyticsWriteClient writerClient;

    private AudienceTagger tagger;
    private RecommendationService svc;

    // No API key → fallback mode
    private final OpenAiProperties props = new OpenAiProperties("", "http://localhost:9999", "gpt-4o-mini", 1024, 0.3);

    @BeforeEach
    void setUp() {
        tagger = new AudienceTagger();
        svc = new RecommendationService(
                analyticsClient, goalsClient, alserClient, travelClient,
                writerClient, tagger, props, null /* not used in fallback */, new ObjectMapper(),
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
    }

    @Test
    void fallbackPath_producesOrderedMatchedOffers() {
        UUID alserOfferId = UUID.randomUUID();
        UUID travelOfferId = UUID.randomUUID();

        UserMetricsDto metrics = new UserMetricsDto(null, "u1", "2026-05",
                new BigDecimal("500000"), null, null, null, null,
                new BigDecimal("0.25"), null, null, null);

        when(analyticsClient.fetchMetrics("u1", "2026-05")).thenReturn(metrics);
        when(goalsClient.fetchGoals("u1")).thenReturn(List.of());
        when(alserClient.fetchOffers(any(Set.class))).thenReturn(List.of(
                new DeviceOfferDto(alserOfferId, "IPHONE-15-PRO", "iPhone 15 Pro",
                        new BigDecimal("750000"), new BigDecimal("10"),
                        List.of("electronics-saver", "saver"), null)
        ));
        when(travelClient.fetchOffers(any(Set.class))).thenReturn(List.of(
                new TravelOfferDto(travelOfferId, "FLIGHT", "Astana → Antalya",
                        new BigDecimal("180000"), new BigDecimal("15"),
                        List.of("saving_for_trip"), null)
        ));

        RecommendationResult result = svc.recommend("u1", "2026-05");

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo("u1");
        assertThat(result.period()).isEqualTo("2026-05");
        assertThat(result.offers()).isNotEmpty();

        // Scores should be in [0,1] range
        result.offers().forEach(offer -> {
            assertThat(offer.score()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            assertThat(offer.score()).isLessThanOrEqualTo(BigDecimal.ONE);
        });

        // Should be sorted descending
        List<MatchedOffer> offers = result.offers();
        for (int i = 0; i < offers.size() - 1; i++) {
            assertThat(offers.get(i).score()).isGreaterThanOrEqualTo(offers.get(i + 1).score());
        }

        // Writer must be called
        verify(writerClient).write(anyString(), anyString(), anyList());
    }

    @Test
    void rankByOverlap_returnsZeroScoreWhenNoTagsMatch() {
        UUID offerId = UUID.randomUUID();
        Set<String> userTags = Set.of("saver");
        List<DeviceOfferDto> alserOffers = List.of(
                new DeviceOfferDto(offerId, "SKU1", "Device",
                        BigDecimal.TEN, BigDecimal.ONE,
                        List.of("high-income"), null));

        List<MatchedOffer> result = svc.rankByOverlap(userTags, alserOffers, List.of());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).score()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void rankByOverlap_returnsMaxScoreWhenAllTagsMatch() {
        UUID offerId = UUID.randomUUID();
        Set<String> userTags = Set.of("saver", "high-income");
        List<DeviceOfferDto> alserOffers = List.of(
                new DeviceOfferDto(offerId, "SKU1", "Device",
                        BigDecimal.TEN, BigDecimal.ONE,
                        List.of("saver", "high-income"), null));

        List<MatchedOffer> result = svc.rankByOverlap(userTags, alserOffers, List.of());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).score()).isEqualByComparingTo(BigDecimal.ONE);
    }
}
