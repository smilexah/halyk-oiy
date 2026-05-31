package kz.halyk.maqsat.recommendation.client;

import java.math.BigDecimal;
import java.util.UUID;

/** Local mirror of goals-service's GoalResponse — only fields needed by recommendation logic. */
public record GoalDto(
        UUID id,
        String name,
        String category,
        BigDecimal targetAmount,
        BigDecimal allocatedAmount,
        BigDecimal progressPercent
) {}
