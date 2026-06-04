package kz.halyk.maqsat.auth.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import kz.halyk.maqsat.auth.entities.enums.Role;

public record InviteRequest(
        @NotNull UUID groupId,
        @NotBlank String username,
        String email,
        String firstName,
        String lastName,
        @NotNull Role role,
        BigDecimal dailyLimit
) {}
