package kz.halyk.maqsat.analytics.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import kz.halyk.maqsat.analytics.domain.TxnFact;
import kz.halyk.maqsat.analytics.repository.TxnFactRepository;
import kz.halyk.maqsat.common.event.TransactionCategorized;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for idempotent ingestion: calling handle() twice with the same transactionId
 * must result in exactly one row in txn_fact.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(IngestService.class)
class IngestServiceTest {

    @Autowired
    private IngestService ingestService;

    @Autowired
    private TxnFactRepository repo;

    @Test
    void duplicateEventIsIngestedOnce() {
        UUID txnId = UUID.randomUUID();
        TransactionCategorized event = new TransactionCategorized(
                txnId,
                "user-1",
                "account-1",
                new BigDecimal("1500.00"),
                "5411",
                "Продукты",
                Instant.parse("2026-05-01T10:00:00Z"),
                "DEBIT",
                "PURCHASE",
                "KZT",
                "Test purchase",
                new BigDecimal("50000.00")
        );

        ingestService.handle(event);
        ingestService.handle(event); // duplicate — must be ignored

        assertThat(repo.count()).isEqualTo(1);
        TxnFact saved = repo.findAll().get(0);
        assertThat(saved.getTransactionId()).isEqualTo(txnId);
        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getCategoryName()).isEqualTo("Продукты");
        assertThat(saved.getCurrency()).isEqualTo("KZT");
        assertThat(saved.getIngestedAt()).isNotNull();
    }
}
