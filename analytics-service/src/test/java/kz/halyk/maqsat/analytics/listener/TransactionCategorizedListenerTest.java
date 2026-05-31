package kz.halyk.maqsat.analytics.listener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import kz.halyk.maqsat.analytics.repository.TxnFactRepository;
import kz.halyk.maqsat.common.event.TransactionCategorized;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end Kafka integration test: sends a TransactionCategorized message via
 * EmbeddedKafka and verifies a row lands in txn_fact.
 *
 * <p>LIMITATION: This test requires a PostgreSQL-compatible database because the
 * V1/V2 Flyway migrations use PostgreSQL-specific DDL (gen_random_uuid(), timestamptz,
 * numeric). H2 in PostgreSQL compat mode does not support all these constructs without
 * Flyway disabled, which then breaks schema creation for JPA. Testcontainers is not
 * currently in the project BOM. To enable this test:
 * <ol>
 *   <li>Add {@code org.testcontainers:testcontainers} and
 *       {@code org.testcontainers:postgresql} to build.gradle.kts testImplementation.</li>
 *   <li>Switch to a {@code @SpringBootTest} with a {@code @DynamicPropertySource} that
 *       points at a Testcontainers PostgreSQL instance.</li>
 *   <li>Remove this {@code @Disabled} annotation.</li>
 * </ol>
 */
@Disabled("Requires Testcontainers PostgreSQL — see class Javadoc for enablement instructions")
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = "transaction.categorized")
class TransactionCategorizedListenerTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private TxnFactRepository repo;

    @Test
    void kafkaMessageLandsInTxnFact() {
        UUID txnId = UUID.randomUUID();
        TransactionCategorized event = new TransactionCategorized(
                txnId,
                "user-kafka",
                "account-kafka",
                new BigDecimal("2000.00"),
                "5411",
                "Продукты",
                Instant.parse("2026-05-15T12:00:00Z"),
                "DEBIT",
                "PURCHASE",
                "KZT",
                null,
                null
        );

        kafkaTemplate.send("transaction.categorized", txnId.toString(), event);

        await().atMost(10, SECONDS)
                .untilAsserted(() -> assertThat(repo.existsByTransactionId(txnId)).isTrue());
    }
}
