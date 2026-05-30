package kz.halyk.maqsat.auth.dto;

import java.util.UUID;

public record InviteResponse(
        String userId,
        String username,
        String temporaryPassword,
        String role,
        UUID groupId,
        String message
) {
}
