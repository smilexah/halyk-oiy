package kz.halyk.maqsat.analytics.service;

import kz.halyk.maqsat.analytics.domain.TxnFact;
import kz.halyk.maqsat.analytics.repository.TxnFactRepository;
import kz.halyk.maqsat.common.event.TransactionCategorized;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestService {

    private final TxnFactRepository repo;

    @Transactional
    public void handle(TransactionCategorized e) {
        if (repo.existsByTransactionId(e.transactionId())) {
            log.debug("Duplicate TransactionCategorized {} — skipped", e.transactionId());
            return;
        }
        TxnFact f = new TxnFact();
        f.setTransactionId(e.transactionId());
        f.setUserId(e.userId());
        f.setAccountId(e.accountId());
        f.setAmount(e.amount());
        f.setMcc(e.mcc());
        f.setCategoryName(e.categoryName());
        f.setOccurredAt(e.occurredAt());
        f.setDirection(e.direction());
        f.setOperationType(e.operationType());
        f.setCurrency(e.currency() != null ? e.currency() : "KZT");
        f.setDetails(e.details());
        f.setBalanceAfter(e.balanceAfter());
        repo.save(f);
        log.info("Ingested txn {} user={} category={}", e.transactionId(), e.userId(), e.categoryName());
    }
}
