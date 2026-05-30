package kz.halyk.maqsat.goals.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import kz.halyk.maqsat.goals.domain.Goal;

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
) {
    public static GoalResponse from(Goal g) {
        BigDecimal progress = BigDecimal.ZERO;
        if (g.getTargetAmount() != null && g.getTargetAmount().signum() > 0) {
            progress = g.getAllocatedAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(g.getTargetAmount(), 2, RoundingMode.HALF_UP);
        }
        BigDecimal balance = g.getVirtualAccount() != null ? g.getVirtualAccount().getBalance() : null;
        return new GoalResponse(
                g.getId(),
                g.getName(),
                g.getDescription(),
                g.getCategory(),
                g.getTargetAmount(),
                g.getAllocatedAmount(),
                g.getMonthlyContribution(),
                progress,
                g.getDeadline(),
                balance,
                g.getBonusProgramRef(),
                g.getCreatedAt());
    }
}