package kz.halyk.maqsat.summary.listeners;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import kz.halyk.maqsat.common.event.MetricsComputed;
import kz.halyk.maqsat.summary.client.AnalyticsClient;
import kz.halyk.maqsat.summary.client.BudgetClient;
import kz.halyk.maqsat.summary.dto.res.SummaryResult;
import kz.halyk.maqsat.summary.event.SummaryEventPublisher;
import kz.halyk.maqsat.summary.services.LanguagePicker;
import kz.halyk.maqsat.summary.services.SummaryLlmService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class MetricsComputedListenerTest {

    @Test
    void onMetrics_callsServiceAndPublishes() {
        var analytics = mock(AnalyticsClient.class);
        var budget = mock(BudgetClient.class);
        var svc = mock(SummaryLlmService.class);
        var languagePicker = mock(LanguagePicker.class);
        var publisher = mock(SummaryEventPublisher.class);

        when(languagePicker.pick("u1")).thenReturn("ru");
        when(analytics.fetchMetrics("u1", "2026-05")).thenReturn(null);
        when(budget.getActive("u1")).thenReturn(null);
        var result = new SummaryResult("ru", "Текст", List.of("h1"), List.of("s1"));
        when(svc.summarise("u1", "2026-05", "ru", null, null)).thenReturn(result);

        var listener = new MetricsComputedListener(analytics, budget, svc, languagePicker, publisher);
        listener.onMetrics(new MetricsComputed("u1", "2026-05",
                new BigDecimal("500000"), new BigDecimal("300000"),
                new BigDecimal("0.1"), new BigDecimal("0.4"), Instant.now()));

        verify(svc).summarise("u1", "2026-05", "ru", null, null);
        verify(publisher).publish("u1", "2026-05", result);
    }

    @Test
    @Disabled("Full EmbeddedKafka test: requires mocking LoadBalanced WebClient beans in @SpringBootTest context")
    void embeddedKafka_integrationTest() {}
}
