package kz.halyk.maqsat.parsebudget.services;

import kz.halyk.maqsat.parsebudget.dto.req.ReplanRequest;
import kz.halyk.maqsat.parsebudget.dto.res.ReplanResponse;

public interface ParseBudgetPlanService {
    ReplanResponse handle(ReplanRequest req);
}
