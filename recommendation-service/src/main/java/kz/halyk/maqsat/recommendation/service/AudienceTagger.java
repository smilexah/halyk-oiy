package kz.halyk.maqsat.recommendation.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import kz.halyk.maqsat.recommendation.client.GoalDto;
import kz.halyk.maqsat.recommendation.client.UserMetricsDto;
import org.springframework.stereotype.Component;

/**
 * Pure function that derives audience tags from user metrics and goals.
 * Null-safe on every field.
 */
@Component
public class AudienceTagger {

    private static final BigDecimal SAVINGS_RATE_SAVER_THRESHOLD = new BigDecimal("0.2");
    private static final BigDecimal VOLATILITY_ERRATIC_THRESHOLD = new BigDecimal("0.3");
    private static final BigDecimal HIGH_INCOME_THRESHOLD = new BigDecimal("600000");

    /**
     * Derives a set of audience tags from the given metrics and goals.
     *
     * @param metrics user metrics (may be null)
     * @param goals   list of user goals (may be null or contain nulls)
     * @return immutable set of tag strings; never null; contains at least "casual" if no other rule fires
     */
    public Set<String> tag(UserMetricsDto metrics, List<GoalDto> goals) {
        Set<String> tags = new HashSet<>();

        if (metrics != null) {
            BigDecimal savingsRate = metrics.savingsRate();
            if (savingsRate != null && savingsRate.compareTo(SAVINGS_RATE_SAVER_THRESHOLD) > 0) {
                tags.add("saver");
            }

            BigDecimal volatility = metrics.volatility();
            if (volatility != null && volatility.compareTo(VOLATILITY_ERRATIC_THRESHOLD) > 0) {
                tags.add("erratic-spender");
            }

            BigDecimal incomeEstimate = metrics.incomeEstimate();
            if (incomeEstimate != null && incomeEstimate.compareTo(HIGH_INCOME_THRESHOLD) > 0) {
                tags.add("high-income");
            }

            // Top spend category by totalAmount
            if (metrics.categories() != null) {
                metrics.categories().stream()
                        .filter(c -> c != null && c.categoryName() != null && c.totalAmount() != null)
                        .max((a, b) -> a.totalAmount().compareTo(b.totalAmount()))
                        .ifPresent(top -> {
                            if ("Рестораны".equalsIgnoreCase(top.categoryName())) {
                                tags.add("foodie");
                            }
                        });
            }
        }

        if (goals != null) {
            for (GoalDto goal : goals) {
                if (goal == null || goal.category() == null) {
                    continue;
                }
                String category = goal.category();
                if ("travel".equalsIgnoreCase(category)) {
                    tags.add("saving_for_trip");
                }
                if ("electronics".equalsIgnoreCase(category) || "electronics-saver".equalsIgnoreCase(category)) {
                    tags.add("electronics-saver");
                }
            }
        }

        if (tags.isEmpty()) {
            tags.add("casual");
        }

        return Collections.unmodifiableSet(tags);
    }
}
