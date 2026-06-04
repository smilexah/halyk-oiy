package kz.halyk.maqsat.goals.repositories;

import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.goals.entities.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<Goal, UUID> {
    List<Goal> findByVirtualAccount_OwnerIdOrderByCreatedAtDesc(String ownerId);
}
