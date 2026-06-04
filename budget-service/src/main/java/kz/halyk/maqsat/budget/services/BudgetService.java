package kz.halyk.maqsat.budget.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import kz.halyk.maqsat.budget.dto.req.CreatePlanRequest;
import kz.halyk.maqsat.budget.dto.req.ParsedPlanPayload;
import kz.halyk.maqsat.budget.dto.res.ActivePlanView;
import kz.halyk.maqsat.budget.dto.res.DashboardResponse;
import kz.halyk.maqsat.budget.entities.BudgetPlan;

public interface BudgetService {
    BudgetPlan createPlan(String currentUserId, CreatePlanRequest request);
    DashboardResponse dashboard(String currentUserId);
    void trackSpending(String ownerId, String categoryName, BigDecimal amount, LocalDate date);
    ActivePlanView getActivePlan(String userId);
    BudgetPlan replan(String userId, ParsedPlanPayload payload);
}
