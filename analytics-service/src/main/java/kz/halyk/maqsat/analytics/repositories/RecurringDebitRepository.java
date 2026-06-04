package kz.halyk.maqsat.analytics.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kz.halyk.maqsat.analytics.entities.RecurringDebit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringDebitRepository extends JpaRepository<RecurringDebit, UUID> {

    List<RecurringDebit> findByUserId(String userId);

    Optional<RecurringDebit> findByUserIdAndLabel(String userId, String label);
}
