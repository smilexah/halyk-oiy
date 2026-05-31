package kz.halyk.maqsat.financial.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kz.halyk.maqsat.common.event.PlanDriftDetected;
import kz.halyk.maqsat.financial.client.AnalyticsClient;
import kz.halyk.maqsat.financial.client.ParseBudgetPlanClient;
import kz.halyk.maqsat.financial.dto.BudgetPlanProposal;
import kz.halyk.maqsat.financial.dto.PlannedCategory;
import kz.halyk.maqsat.financial.dto.ReplanRequest;
import kz.halyk.maqsat.financial.dto.ReplanResponse;
import kz.halyk.maqsat.financial.dto.UserMetricsDto;
import kz.halyk.maqsat.financial.service.FinancialAgentService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integration-level Kafka test disabled because @EmbeddedKafka in this project's configuration
 * requires Testcontainers or a running broker; the H2 + embedded-kafka combo has classpath conflicts
 * with the spring-kafka-test version pinned in the root build. Unit-level coverage of the listener
 * logic is provided by the method-level test below.
 *
 * See also: analytics-service/src/test/java/kz/halyk/maqsat/analytics/listener for the same pattern.
 */
class PlanDriftListenerTest {

    @Test
    void onDrift_whenMetricsPresent_callsParserWithProposal() {
        AnalyticsClient analytics = mock(AnalyticsClient.class);
        FinancialAgentService agent = mock(FinancialAgentService.class);
        ParseBudgetPlanClient parser = mock(ParseBudgetPlanClient.class);

        UserMetricsDto metrics = new UserMetricsDto(
                UUID.randomUUID(), "u1", "2026-05",
                new BigDecimal("500000"), new BigDecimal("300000"),
                new BigDecimal("10000"), new BigDecimal("9000"),
                new BigDecimal("0.1"), new BigDecimal("0.4"),
                Instant.now(), List.of(), List.of());

        BudgetPlanProposal proposal = new BudgetPlanProposal(
                List.of(new PlannedCategory("Продукты", "MANDATORY", new BigDecimal("125000"))),
                "fallback");

        when(analytics.fetchMetrics("u1", "2026-05")).thenReturn(metrics);
        when(agent.propose(anyString(), anyString(), anyString(), any(), any())).thenReturn(proposal);
        when(parser.replan(any())).thenReturn(new ReplanResponse(UUID.randomUUID(), 2, UUID.randomUUID()));

        PlanDriftListener listener = new PlanDriftListener(analytics, agent, parser, new io.micrometer.core.instrument.simple.SimpleMeterRegistry());

        PlanDriftDetected event = new PlanDriftDetected(
                "u1", UUID.randomUUID(), "2026-05",
                Map.of("Рестораны", new BigDecimal("0.30")),
                true, Instant.now());

        listener.onDrift(event);

        verify(parser).replan(any(ReplanRequest.class));
    }

    @Test
    void onDrift_whenNoMetrics_skipsReplan() {
        AnalyticsClient analytics = mock(AnalyticsClient.class);
        FinancialAgentService agent = mock(FinancialAgentService.class);
        ParseBudgetPlanClient parser = mock(ParseBudgetPlanClient.class);

        when(analytics.fetchMetrics("u1", "2026-05")).thenReturn(null);

        PlanDriftListener listener = new PlanDriftListener(analytics, agent, parser, new io.micrometer.core.instrument.simple.SimpleMeterRegistry());

        PlanDriftDetected event = new PlanDriftDetected(
                "u1", UUID.randomUUID(), "2026-05",
                Map.of(), true, Instant.now());

        listener.onDrift(event);

        // No replan call when metrics are absent
        verify(parser, org.mockito.Mockito.never()).replan(any());
    }

    @Disabled("@EmbeddedKafka test disabled: requires Testcontainers or broker; classpath conflict " +
              "with spring-kafka-test in this Gradle multi-module setup. See class Javadoc.")
    @Test
    void embeddedKafka_planDriftDetected_triggersReplan() {
        // Placeholder: would use @EmbeddedKafka + KafkaTemplate to publish PlanDriftDetected,
        // then await via Awaitility that ParseBudgetPlanClient.replan() was invoked.
    }
}
