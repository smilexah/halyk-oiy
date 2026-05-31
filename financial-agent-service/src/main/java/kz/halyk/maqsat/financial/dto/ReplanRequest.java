package kz.halyk.maqsat.financial.dto;

import java.math.BigDecimal;
import java.util.Map;

public record ReplanRequest(
        String userId,
        String period,
        BudgetPlanProposal proposal,
        Map<String, BigDecimal> driftSnapshot
) {}
