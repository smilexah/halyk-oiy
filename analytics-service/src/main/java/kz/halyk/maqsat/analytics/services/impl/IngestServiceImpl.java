package kz.halyk.maqsat.analytics.services.impl;

import kz.halyk.maqsat.analytics.mappers.TxnFactMapper;
import kz.halyk.maqsat.analytics.repositories.TxnFactRepository;
import kz.halyk.maqsat.analytics.services.IngestService;
import kz.halyk.maqsat.common.event.TransactionCategorized;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestServiceImpl implements IngestService {

    private final TxnFactRepository repo;
    private final TxnFactMapper txnFactMapper;

    @Override
    @Transactional
    public void handle(TransactionCategorized e) {
        if (repo.existsByTransactionId(e.transactionId())) {
            log.debug("Duplicate TransactionCategorized {} — skipped", e.transactionId());
            return;
        }
        repo.save(txnFactMapper.toEntity(e));
        log.info("Ingested txn {} user={} category={}", e.transactionId(), e.userId(), e.categoryName());
    }
}
