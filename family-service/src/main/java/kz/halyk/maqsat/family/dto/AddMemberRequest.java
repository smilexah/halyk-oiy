package kz.halyk.maqsat.family.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import kz.halyk.maqsat.family.domain.Role;

public record AddMemberRequest(
        @NotBlank String userId,
        @NotNull Role role,
        BigDecimal dailyLimit
) {
}
