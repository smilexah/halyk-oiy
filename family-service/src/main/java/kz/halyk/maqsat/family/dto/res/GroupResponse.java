package kz.halyk.maqsat.family.dto.res;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GroupResponse(
        UUID id,
        String name,
        String type,
        String createdBy,
        Instant createdAt,
        List<MemberView> members
) {
    public record MemberView(
            UUID membershipId,
            String userId,
            String role,
            BigDecimal dailyLimit
    ) {}
}
