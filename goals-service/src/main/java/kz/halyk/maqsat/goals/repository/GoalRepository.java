package kz.halyk.maqsat.goals.repository;

import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.goals.domain.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findByVirtualAccount_OwnerIdOrderByCreatedAtDesc(String ownerId);
}