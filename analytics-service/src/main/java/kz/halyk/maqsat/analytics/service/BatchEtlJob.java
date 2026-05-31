package kz.halyk.maqsat.analytics.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import kz.halyk.maqsat.analytics.client.TransactionsClient;
import kz.halyk.maqsat.analytics.repository.TxnFactRepository;
import kz.halyk.maqsat.common.event.TransactionCategorized;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BatchEtlJob {

    private final TransactionsClient client;
    private final IngestService ingest;
    private final TxnFactRepository repo;

    /**
     * Nightly back-fill: pulls transactions from transaction-service starting from the
     * high-water-mark (last ingestedAt), or 30 days back if no rows exist yet.
     * Cron defaults to 02:00 UTC daily; override with {@code analytics.etl.cron}.
     */
    @Scheduled(cron = "${analytics.etl.cron:0 0 2 * * *}")
    public void run() {
        Instant since = repo.findTopByOrderByIngestedAtDesc()
                .map(kz.halyk.maqsat.analytics.domain.TxnFact::getIngestedAt)
                .orElse(Instant.now().minus(Duration.ofDays(30)));
        List<TransactionCategorized> events = client.fetchSince(since);
        if (events != null) {
            events.forEach(ingest::handle);
        }
        log.info("Batch ETL pulled {} txns since {}", events == null ? 0 : events.size(), since);
    }
}
