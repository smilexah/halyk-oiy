package kz.halyk.maqsat.goals.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.goals.domain.Goal;
import kz.halyk.maqsat.goals.domain.OwnerType;
import kz.halyk.maqsat.goals.domain.VirtualAccount;
import kz.halyk.maqsat.goals.dto.CreateGoalRequest;
import kz.halyk.maqsat.goals.exception.GoalNotFoundException;
import kz.halyk.maqsat.goals.repository.GoalRepository;
import kz.halyk.maqsat.goals.repository.VirtualAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final VirtualAccountRepository virtualAccountRepository;

    @Transactional
    public Goal createGoal(String ownerId, CreateGoalRequest request) {
        VirtualAccount account = new VirtualAccount();
        account.setOwnerId(ownerId);
        account.setOwnerType(OwnerType.USER);
        account.setBalance(BigDecimal.ZERO);
        virtualAccountRepository.save(account);

        Goal goal = new Goal();
        goal.setName(request.name());
        goal.setDescription(request.description());
        goal.setCategory(request.category());
        goal.setTargetAmount(request.targetAmount());
        goal.setAllocatedAmount(BigDecimal.ZERO);
        goal.setMonthlyContribution(request.monthlyContribution());
        goal.setDeadline(request.deadline());
        goal.setBonusProgramRef(request.bonusProgramRef());
        goal.setVirtualAccount(account);
        goal.setCreatedAt(Instant.now());
        return goalRepository.save(goal);
    }

    @Transactional(readOnly = true)
    public List<Goal> listForUser(String ownerId) {
        return goalRepository.findByVirtualAccount_OwnerIdOrderByCreatedAtDesc(ownerId);
    }

    @Transactional(readOnly = true)
    public List<Goal> internalListForUser(String userId) {
        return goalRepository.findByVirtualAccount_OwnerIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Goal contribute(String ownerId, UUID goalId, BigDecimal amount) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new GoalNotFoundException("Goal not found: " + goalId));
        if (goal.getVirtualAccount() == null || !ownerId.equals(goal.getVirtualAccount().getOwnerId())) {
            throw new GoalNotFoundException("Goal not found: " + goalId);
        }
        goal.setAllocatedAmount(goal.getAllocatedAmount().add(amount));
        VirtualAccount account = goal.getVirtualAccount();
        account.setBalance(account.getBalance().add(amount));
        return goal;
    }
}