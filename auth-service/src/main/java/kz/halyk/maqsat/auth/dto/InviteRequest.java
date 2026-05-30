package kz.halyk.maqsat.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record InviteRequest(
        @NotNull UUID groupId,
        @NotBlank String username,
        String email,
        String firstName,
        String lastName,
        @NotNull Role role,
        BigDecimal dailyLimit
) {
}
