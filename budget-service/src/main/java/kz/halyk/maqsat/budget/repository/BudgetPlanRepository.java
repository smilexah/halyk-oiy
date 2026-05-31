package kz.halyk.maqsat.budget.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import kz.halyk.maqsat.budget.domain.BudgetPlan;
import kz.halyk.maqsat.budget.domain.OwnerType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetPlanRepository extends JpaRepository<BudgetPlan, UUID> {

    Optional<BudgetPlan> findFirstByOwnerIdAndOwnerTypeAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqualOrderByPeriodStartDesc(
            String ownerId, OwnerType ownerType, LocalDate start, LocalDate end);

    Optional<BudgetPlan> findFirstByOwnerIdAndOwnerTypeAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqualAndSupersededByIsNullOrderByPeriodStartDesc(
            String ownerId, OwnerType ownerType, LocalDate start, LocalDate end);

    Optional<BudgetPlan> findFirstByOwnerIdAndOwnerTypeAndSupersededByIsNullOrderByVersionDesc(
            String ownerId, OwnerType ownerType);
}