package kz.halyk.maqsat.parsebudget.dto.res;

import java.util.UUID;

public record ReplanResponse(UUID newPlanId, Integer version, UUID supersededPlanId) {}
