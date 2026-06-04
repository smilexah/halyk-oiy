package kz.halyk.maqsat.analytics.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kz.halyk.maqsat.analytics.entities.TxnFact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TxnFactRepository extends JpaRepository<TxnFact, UUID> {

    boolean existsByTransactionId(UUID transactionId);

    List<TxnFact> findByUserIdAndOccurredAtBetween(String userId, Instant from, Instant to);

    Optional<TxnFact> findTopByOrderByIngestedAtDesc();
}
