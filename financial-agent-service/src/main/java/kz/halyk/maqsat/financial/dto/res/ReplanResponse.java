package kz.halyk.maqsat.financial.dto.res;

import java.util.UUID;

public record ReplanResponse(UUID newPlanId, Integer version, UUID supersededPlanId) {}
