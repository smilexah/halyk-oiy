package kz.halyk.maqsat.parsebudget.dto;

import java.util.UUID;

public record ReplanResponse(UUID newPlanId, Integer version, UUID supersededPlanId) {}
