package kz.halyk.maqsat.goals.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.goals.dto.req.CreateGoalRequest;
import kz.halyk.maqsat.goals.entities.Goal;

public interface GoalService {
    Goal createGoal(String ownerId, CreateGoalRequest request);
    List<Goal> listForUser(String ownerId);
    List<Goal> internalListForUser(String userId);
    Goal contribute(String ownerId, UUID goalId, BigDecimal amount);
}
