package kz.halyk.maqsat.goals.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateGoalRequest(
        @NotBlank String name,
        String description,
        String category,
        @NotNull @Positive BigDecimal targetAmount,
        BigDecimal monthlyContribution,
        LocalDate deadline,
        String bonusProgramRef
) {}
