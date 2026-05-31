package kz.halyk.maqsat.transaction.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.transaction.domain.Transaction;
import kz.halyk.maqsat.transaction.domain.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByUserIdOrderByOccurredAtDesc(String userId);

    /** Used by analytics-service batch ETL to back-fill transactions from a given point in time. */
    List<Transaction> findByOccurredAtGreaterThanEqualOrderByOccurredAtAsc(Instant since);

    @Query("""
            select coalesce(sum(t.amount), 0)
            from Transaction t
            where t.userId = :userId
              and t.status = :status
              and t.occurredAt >= :from
              and t.occurredAt < :to
            """)
    BigDecimal sumAmountForUserBetween(@Param("userId") String userId,
                                       @Param("status") TransactionStatus status,
                                       @Param("from") Instant from,
                                       @Param("to") Instant to);
}
