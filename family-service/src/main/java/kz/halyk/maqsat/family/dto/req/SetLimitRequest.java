package kz.halyk.maqsat.family.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record SetLimitRequest(@NotNull @Positive BigDecimal dailyLimit) {}
