package kz.halyk.maqsat.financial.listeners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import kz.halyk.maqsat.financial.dto.req.ReplanRequest;
import kz.halyk.maqsat.financial.dto.res.BudgetPlanProposal;
import kz.halyk.maqsat.financial.dto.res.PlannedCategory;
import kz.halyk.maqsat.financial.dto.res.ReplanResponse;
import kz.halyk.maqsat.financial.dto.res.UserMetricsDto;
import kz.halyk.maqsat.financial.services.FinancialAgentService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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

        PlanDriftListener listener = new PlanDriftListener(analytics, agent, parser,
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry());

        listener.onDrift(new PlanDriftDetected("u1", UUID.randomUUID(), "2026-05",
                Map.of("Рестораны", new BigDecimal("0.30")), true, Instant.now()));

        verify(parser).replan(any(ReplanRequest.class));
    }

    @Test
    void onDrift_whenNoMetrics_skipsReplan() {
        AnalyticsClient analytics = mock(AnalyticsClient.class);
        FinancialAgentService agent = mock(FinancialAgentService.class);
        ParseBudgetPlanClient parser = mock(ParseBudgetPlanClient.class);

        when(analytics.fetchMetrics("u1", "2026-05")).thenReturn(null);

        PlanDriftListener listener = new PlanDriftListener(analytics, agent, parser,
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry());

        listener.onDrift(new PlanDriftDetected("u1", UUID.randomUUID(), "2026-05",
                Map.of(), true, Instant.now()));

        verify(parser, never()).replan(any());
    }

    @Disabled("@EmbeddedKafka test disabled — classpath conflict with spring-kafka-test in multi-module Gradle setup")
    @Test
    void embeddedKafka_planDriftDetected_triggersReplan() {}
}
