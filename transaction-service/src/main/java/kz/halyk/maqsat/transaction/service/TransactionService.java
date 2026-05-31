package kz.halyk.maqsat.transaction.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kz.halyk.maqsat.transaction.client.FamilyClient;
import kz.halyk.maqsat.transaction.domain.Direction;
import kz.halyk.maqsat.transaction.domain.OperationType;
import kz.halyk.maqsat.transaction.domain.Transaction;
import kz.halyk.maqsat.transaction.domain.TransactionStatus;
import kz.halyk.maqsat.transaction.dto.ChildLimitView;
import kz.halyk.maqsat.transaction.dto.TransactionRequest;
import kz.halyk.maqsat.transaction.dto.TransactionResponse;
import kz.halyk.maqsat.transaction.event.TransactionEventPublisher;
import kz.halyk.maqsat.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository repository;
    private final CategorizationEngine categorizationEngine;
    private final TransactionEventPublisher publisher;
    private final FamilyClient familyClient;
    private final MeterRegistry meterRegistry;

    @Transactional
    public Transaction ingest(String userId, String bearer, TransactionRequest request) {
        Transaction txn = new Transaction();
        txn.setUserId(userId);
        txn.setAccountId(request.accountId());
        txn.setAmount(request.amount());
        txn.setMerchant(request.merchant());
        txn.setMcc(request.mcc());
        txn.setOccurredAt(request.occurredAt() != null ? request.occurredAt() : Instant.now());
        txn.setCategoryName(categorizationEngine.categorize(request.mcc(), request.merchant()));
        txn.setDirection(request.direction() != null ? request.direction() : Direction.DEBIT);
        txn.setOperationType(request.operationType() != null ? request.operationType() : OperationType.PURCHASE);
        txn.setCurrency(request.currency() != null ? request.currency() : "KZT");
        txn.setDetails(request.details());
        txn.setBalanceAfter(request.balanceAfter());

        ChildLimitView limit = familyClient.getChildLimit(bearer, userId);
        BigDecimal shortfall = childShortfall(userId, txn, limit);

        if (shortfall != null && shortfall.signum() > 0) {
            // Soft block: hold for parental approval instead of declining.
            txn.setStatus(TransactionStatus.PENDING_APPROVAL);
            repository.save(txn);
            publisher.publishLimitExceeded(txn, effectiveLimit(limit), shortfall);
            meterRegistry.counter("maqsat.transactions", "status", "PENDING_APPROVAL").increment();
            meterRegistry.counter("maqsat.limit.exceeded").increment();
            log.info("Child {} hit daily limit; txn {} is PENDING_APPROVAL (shortfall {})", userId, txn.getId(), shortfall);
            return txn;
        }

        txn.setStatus(TransactionStatus.POSTED);
        repository.save(txn);
        publisher.publishCategorized(txn);
        meterRegistry.counter("maqsat.transactions", "status", "POSTED").increment();
        return txn;
    }

    /** Returns the amount by which this transaction exceeds the child's remaining daily allowance, or null if not a limited child. */
    private BigDecimal childShortfall(String userId, Transaction txn, ChildLimitView limit) {
        if (limit == null || limit.dailyLimit() == null) {
            return null;
        }
        Instant dayStart = txn.getOccurredAt().atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = dayStart.plusSeconds(86_400);
        BigDecimal spentToday = repository.sumAmountForUserBetween(userId, TransactionStatus.POSTED, dayStart, dayEnd);
        BigDecimal allowance = effectiveLimit(limit);
        BigDecimal projected = spentToday.add(txn.getAmount());
        return projected.subtract(allowance); // positive => exceeds
    }

    private BigDecimal effectiveLimit(ChildLimitView limit) {
        BigDecimal base = limit.dailyLimit();
        if (limit.overrideUntil() != null && limit.overrideUntil().isAfter(Instant.now()) && limit.overrideAmount() != null) {
            return base.add(limit.overrideAmount());
        }
        return base;
    }

    /** Post a previously held transaction after a parent approves the override. */
    @Transactional
    public void approve(UUID transactionId) {
        Optional<Transaction> maybe = repository.findById(transactionId);
        if (maybe.isEmpty()) {
            log.warn("Approval for unknown transaction {}", transactionId);
            return;
        }
        Transaction txn = maybe.get();
        if (txn.getStatus() != TransactionStatus.PENDING_APPROVAL) {
            log.info("Transaction {} not pending (status {}), ignoring approval", transactionId, txn.getStatus());
            return;
        }
        txn.setStatus(TransactionStatus.POSTED);
        repository.save(txn);
        publisher.publishCategorized(txn);
        meterRegistry.counter("maqsat.transactions.approved").increment();
        log.info("Transaction {} approved and POSTED", transactionId);
    }

    @Transactional(readOnly = true)
    public List<Transaction> listForUser(String userId) {
        return repository.findByUserIdOrderByOccurredAtDesc(userId);
    }

    /** Returns all POSTED transactions with occurredAt >= since, ordered ascending. Used by analytics-service ETL. */
    @Transactional(readOnly = true)
    public List<TransactionResponse> findSince(Instant since) {
        return repository.findByOccurredAtGreaterThanEqualOrderByOccurredAtAsc(since)
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }
}
