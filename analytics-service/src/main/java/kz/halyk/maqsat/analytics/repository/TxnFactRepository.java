package kz.halyk.maqsat.analytics.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kz.halyk.maqsat.analytics.domain.TxnFact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TxnFactRepository extends JpaRepository<TxnFact, UUID> {

    boolean existsByTransactionId(UUID transactionId);

    List<TxnFact> findByUserIdAndOccurredAtBetween(String userId, Instant from, Instant to);

    /** Returns the most recently ingested fact — used by the batch ETL job as a high-water mark. */
    Optional<TxnFact> findTopByOrderByIngestedAtDesc();
}
