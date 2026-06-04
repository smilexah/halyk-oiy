package kz.halyk.maqsat.goals.dto.res;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GoalResponse(
        UUID id,
        String name,
        String description,
        String category,
        BigDecimal targetAmount,
        BigDecimal allocatedAmount,
        BigDecimal monthlyContribution,
        BigDecimal progressPercent,
        LocalDate deadline,
        BigDecimal virtualAccountBalance,
        String bonusProgramRef,
        Instant createdAt
) {}
