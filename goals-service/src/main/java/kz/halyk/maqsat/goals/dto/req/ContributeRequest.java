package kz.halyk.maqsat.goals.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ContributeRequest(@NotNull @Positive BigDecimal amount) {}
