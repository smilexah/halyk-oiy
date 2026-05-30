package kz.halyk.maqsat.family.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.family.domain.FamilyGroup;

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
    ) {
    }

    public static GroupResponse from(FamilyGroup g) {
        List<MemberView> members = g.getMemberships().stream()
                .map(m -> new MemberView(
                        m.getId(),
                        m.getUserId(),
                        m.getRole().name(),
                        m.getChildLimit() != null ? m.getChildLimit().getDailyLimit() : null))
                .toList();
        return new GroupResponse(g.getId(), g.getName(), g.getType(), g.getCreatedBy(), g.getCreatedAt(), members);
    }
}
