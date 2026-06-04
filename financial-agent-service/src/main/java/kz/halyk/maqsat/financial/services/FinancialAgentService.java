package kz.halyk.maqsat.financial.services;

import java.math.BigDecimal;
import java.util.Map;
import kz.halyk.maqsat.financial.dto.res.BudgetPlanProposal;

public interface FinancialAgentService {
    BudgetPlanProposal propose(String userId, String period, String segmentTag,
                               Map<String, BigDecimal> metrics,
                               Map<String, BigDecimal> driftByCategory);
}
