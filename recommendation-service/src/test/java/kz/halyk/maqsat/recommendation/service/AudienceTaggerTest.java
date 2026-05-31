package kz.halyk.maqsat.recommendation.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import kz.halyk.maqsat.recommendation.client.GoalDto;
import kz.halyk.maqsat.recommendation.client.UserMetricsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class AudienceTaggerTest {

    private AudienceTagger tagger;

    @BeforeEach
    void setUp() {
        tagger = new AudienceTagger();
    }

    @Test
    void saverTag_whenSavingsRateAboveThreshold() {
        UserMetricsDto metrics = metricsWithSavingsRate(new BigDecimal("0.25"));
        Set<String> tags = tagger.tag(metrics, Collections.emptyList());
        assertThat(tags).contains("saver");
    }

    @Test
    void noSaverTag_whenSavingsRateBelowThreshold() {
        UserMetricsDto metrics = metricsWithSavingsRate(new BigDecimal("0.15"));
        Set<String> tags = tagger.tag(metrics, Collections.emptyList());
        assertThat(tags).doesNotContain("saver");
    }

    @Test
    void erraticSpenderTag_whenVolatilityAboveThreshold() {
        UserMetricsDto metrics = metricsWithVolatility(new BigDecimal("0.5"));
        Set<String> tags = tagger.tag(metrics, Collections.emptyList());
        assertThat(tags).contains("erratic-spender");
    }

    @Test
    void savingForTripTag_whenGoalCategoryIsTravel() {
        GoalDto goal = new GoalDto(null, "Bali Trip", "travel",
                new BigDecimal("500000"), new BigDecimal("100000"), new BigDecimal("20"));
        Set<String> tags = tagger.tag(null, List.of(goal));
        assertThat(tags).contains("saving_for_trip");
    }

    @Test
    void foodieTag_whenTopSpendCategoryIsRestaurants() {
        UserMetricsDto.CategoryStatView restaurants = new UserMetricsDto.CategoryStatView(
                null, "Рестораны", 10, new BigDecimal("50000"), new BigDecimal("5000"), new BigDecimal("4500"));
        UserMetricsDto.CategoryStatView products = new UserMetricsDto.CategoryStatView(
                null, "Продукты", 5, new BigDecimal("30000"), new BigDecimal("6000"), new BigDecimal("5500"));
        UserMetricsDto metrics = new UserMetricsDto(null, "u1", "2026-05",
                null, null, null, null, null, null, null,
                List.of(restaurants, products), null);
        Set<String> tags = tagger.tag(metrics, Collections.emptyList());
        assertThat(tags).contains("foodie");
    }

    @Test
    void highIncomeTag_whenIncomeEstimateAboveThreshold() {
        UserMetricsDto metrics = metricsWithIncome(new BigDecimal("700000"));
        Set<String> tags = tagger.tag(metrics, Collections.emptyList());
        assertThat(tags).contains("high-income");
    }

    @Test
    void electronicsSaverTag_whenGoalCategoryIsElectronics() {
        GoalDto goal = new GoalDto(null, "New MacBook", "electronics",
                new BigDecimal("800000"), new BigDecimal("200000"), new BigDecimal("25"));
        Set<String> tags = tagger.tag(null, List.of(goal));
        assertThat(tags).contains("electronics-saver");
    }

    @Test
    void casualTag_whenNoRulesFire() {
        Set<String> tags = tagger.tag(null, Collections.emptyList());
        assertThat(tags).containsExactly("casual");
    }

    @Test
    void nullSafe_neverThrowsOnNullInputs() {
        assertThatNoException().isThrownBy(() -> tagger.tag(null, null));
        assertThatNoException().isThrownBy(() -> tagger.tag(
                new UserMetricsDto(null, null, null, null, null, null, null, null, null, null, null, null),
                List.of(new GoalDto(null, null, null, null, null, null))));
    }

    // --- helpers ---

    private UserMetricsDto metricsWithSavingsRate(BigDecimal rate) {
        return new UserMetricsDto(null, "u1", "2026-05",
                null, null, null, null, null, rate, null, null, null);
    }

    private UserMetricsDto metricsWithVolatility(BigDecimal volatility) {
        return new UserMetricsDto(null, "u1", "2026-05",
                null, null, null, null, volatility, null, null, null, null);
    }

    private UserMetricsDto metricsWithIncome(BigDecimal income) {
        return new UserMetricsDto(null, "u1", "2026-05",
                income, null, null, null, null, null, null, null, null);
    }
}
