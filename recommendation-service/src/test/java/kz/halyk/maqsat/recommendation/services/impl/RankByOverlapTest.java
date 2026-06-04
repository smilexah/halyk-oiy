package kz.halyk.maqsat.recommendation.services.impl;

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
import kz.halyk.maqsat.recommendation.config.OpenAiProperties;
import kz.halyk.maqsat.recommendation.dto.res.MatchedOffer;
import kz.halyk.maqsat.recommendation.services.AudienceTagger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class RankByOverlapTest {

    private final OpenAiProperties props = new OpenAiProperties("", "http://localhost:9999", "gpt-4o-mini", 1024, 0.3);

    private RecommendationServiceImpl svc() {
        return new RecommendationServiceImpl(
                mock(AnalyticsClient.class), mock(GoalsClient.class),
                mock(AlserClient.class), mock(HalykTravelClient.class),
                mock(AnalyticsWriteClient.class), new AudienceTagger(),
                props, null, new ObjectMapper(),
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
    }

    @Test
    void returnsZeroScoreWhenNoTagsMatch() {
        UUID offerId = UUID.randomUUID();
        List<MatchedOffer> result = svc().rankByOverlap(
                Set.of("saver"),
                List.of(new DeviceOfferDto(offerId, "SKU1", "Device", BigDecimal.TEN, BigDecimal.ONE, List.of("high-income"), null)),
                List.of());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).score()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void returnsMaxScoreWhenAllTagsMatch() {
        UUID offerId = UUID.randomUUID();
        List<MatchedOffer> result = svc().rankByOverlap(
                Set.of("saver", "high-income"),
                List.of(new DeviceOfferDto(offerId, "SKU1", "Device", BigDecimal.TEN, BigDecimal.ONE, List.of("saver", "high-income"), null)),
                List.of());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).score()).isEqualByComparingTo(BigDecimal.ONE);
    }
}
