package kz.halyk.maqsat.analytics.listeners;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import kz.halyk.maqsat.analytics.repositories.TxnFactRepository;
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
                txnId, "user-kafka", "account-kafka",
                new BigDecimal("2000.00"), "5411", "Продукты",
                Instant.parse("2026-05-15T12:00:00Z"),
                "DEBIT", "PURCHASE", "KZT", null, null);

        kafkaTemplate.send("transaction.categorized", txnId.toString(), event);

        await().atMost(10, SECONDS)
                .untilAsserted(() -> assertThat(repo.existsByTransactionId(txnId)).isTrue());
    }
}
