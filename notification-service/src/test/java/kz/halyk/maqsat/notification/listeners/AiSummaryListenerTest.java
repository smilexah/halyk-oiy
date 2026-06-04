package kz.halyk.maqsat.notification.listeners;

import java.time.Instant;
import java.util.List;
import kz.halyk.maqsat.common.event.SummaryGenerated;
import org.junit.jupiter.api.Test;

class AiSummaryListenerTest {

    @Test
    void doesNotThrowOnValidEvent() {
        var listener = new AiSummaryListener();
        listener.onSummary(new SummaryGenerated("u1", "2026-05", "ru",
                "Текст", List.of("h1"), List.of("s1"), Instant.now()));
    }
}
