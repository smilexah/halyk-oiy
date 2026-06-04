package kz.halyk.maqsat.recommendation.services;

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
        assertThat(tagger.tag(metricsWithSavingsRate(new BigDecimal("0.25")), Collections.emptyList()))
                .contains("saver");
    }

    @Test
    void noSaverTag_whenSavingsRateBelowThreshold() {
        assertThat(tagger.tag(metricsWithSavingsRate(new BigDecimal("0.15")), Collections.emptyList()))
                .doesNotContain("saver");
    }

    @Test
    void erraticSpenderTag_whenVolatilityAboveThreshold() {
        assertThat(tagger.tag(metricsWithVolatility(new BigDecimal("0.5")), Collections.emptyList()))
                .contains("erratic-spender");
    }

    @Test
    void savingForTripTag_whenGoalCategoryIsTravel() {
        GoalDto goal = new GoalDto(null, "Bali Trip", "travel",
                new BigDecimal("500000"), new BigDecimal("100000"), new BigDecimal("20"));
        assertThat(tagger.tag(null, List.of(goal))).contains("saving_for_trip");
    }

    @Test
    void foodieTag_whenTopSpendCategoryIsRestaurants() {
        UserMetricsDto.CategoryStatView restaurants = new UserMetricsDto.CategoryStatView(
                null, "Рестораны", 10, new BigDecimal("50000"), new BigDecimal("5000"), new BigDecimal("4500"));
        UserMetricsDto.CategoryStatView products = new UserMetricsDto.CategoryStatView(
                null, "Продукты", 5, new BigDecimal("30000"), new BigDecimal("6000"), new BigDecimal("5500"));
        UserMetricsDto metrics = new UserMetricsDto(null, "u1", "2026-05",
                null, null, null, null, null, null, null, List.of(restaurants, products), null);
        assertThat(tagger.tag(metrics, Collections.emptyList())).contains("foodie");
    }

    @Test
    void highIncomeTag_whenIncomeEstimateAboveThreshold() {
        assertThat(tagger.tag(metricsWithIncome(new BigDecimal("700000")), Collections.emptyList()))
                .contains("high-income");
    }

    @Test
    void electronicsSaverTag_whenGoalCategoryIsElectronics() {
        GoalDto goal = new GoalDto(null, "New MacBook", "electronics",
                new BigDecimal("800000"), new BigDecimal("200000"), new BigDecimal("25"));
        assertThat(tagger.tag(null, List.of(goal))).contains("electronics-saver");
    }

    @Test
    void casualTag_whenNoRulesFire() {
        assertThat(tagger.tag(null, Collections.emptyList())).containsExactly("casual");
    }

    @Test
    void nullSafe_neverThrowsOnNullInputs() {
        assertThatNoException().isThrownBy(() -> tagger.tag(null, null));
        assertThatNoException().isThrownBy(() -> tagger.tag(
                new UserMetricsDto(null, null, null, null, null, null, null, null, null, null, null, null),
                List.of(new GoalDto(null, null, null, null, null, null))));
    }

    private UserMetricsDto metricsWithSavingsRate(BigDecimal rate) {
        return new UserMetricsDto(null, "u1", "2026-05", null, null, null, null, null, rate, null, null, null);
    }

    private UserMetricsDto metricsWithVolatility(BigDecimal volatility) {
        return new UserMetricsDto(null, "u1", "2026-05", null, null, null, null, volatility, null, null, null, null);
    }

    private UserMetricsDto metricsWithIncome(BigDecimal income) {
        return new UserMetricsDto(null, "u1", "2026-05", income, null, null, null, null, null, null, null, null);
    }
}
