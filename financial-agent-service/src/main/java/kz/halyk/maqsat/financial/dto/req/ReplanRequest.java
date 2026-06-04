package kz.halyk.maqsat.financial.dto.req;

import java.math.BigDecimal;
import java.util.Map;
import kz.halyk.maqsat.financial.dto.res.BudgetPlanProposal;

public record ReplanRequest(
        String userId,
        String period,
        BudgetPlanProposal proposal,
        Map<String, BigDecimal> driftSnapshot
) {}
