package kz.halyk.maqsat.financial.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;
import kz.halyk.maqsat.financial.client.OpenAiClient;
import kz.halyk.maqsat.financial.config.OpenAiProperties;
import kz.halyk.maqsat.financial.dto.BudgetPlanProposal;
import org.junit.jupiter.api.Test;

class FinancialAgentServiceTest {

    @Test
    void fallback_whenNoApiKey_usesPriorsBaseline() {
        var props = new OpenAiProperties("", "http://localhost", "gpt-4o-mini", 1024, 0.2);
        var priorsMock = mock(PriorsService.class);
        var openAiMock = mock(OpenAiClient.class);

        when(priorsMock.getPriorsFor("default")).thenReturn(Map.of(
                "Продукты", new BigDecimal("0.25"),
                "Рестораны", new BigDecimal("0.10")));

        var svc = new FinancialAgentService(props, openAiMock, priorsMock, new io.micrometer.core.instrument.simple.SimpleMeterRegistry());

        Map<String, BigDecimal> metrics = Map.of("incomeEstimate", new BigDecimal("500000"));
        Map<String, BigDecimal> drift = Map.of("Рестораны", new BigDecimal("0.30"));

        BudgetPlanProposal p = svc.propose("u1", "2026-05", "default", metrics, drift);

        assertThat(p.categories()).hasSize(2);
        assertThat(p.categories().stream()
                .filter(c -> c.name().equals("Продукты"))
                .findFirst().orElseThrow()
                .limitAmount()).isEqualByComparingTo("125000");
        assertThat(p.rationale()).contains("fallback");

        // LLM must NOT be called when no API key
        verify(openAiMock, never()).complete(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void fallback_zeroIncome_producesZeroLimits() {
        var props = new OpenAiProperties("", "http://localhost", "gpt-4o-mini", 1024, 0.2);
        var priorsMock = mock(PriorsService.class);
        var openAiMock = mock(OpenAiClient.class);

        when(priorsMock.getPriorsFor("default")).thenReturn(Map.of(
                "Продукты", new BigDecimal("0.25")));

        var svc = new FinancialAgentService(props, openAiMock, priorsMock, new io.micrometer.core.instrument.simple.SimpleMeterRegistry());

        BudgetPlanProposal p = svc.propose("u1", "2026-05", "default",
                Map.of("incomeEstimate", BigDecimal.ZERO), Map.of());

        assertThat(p.categories().get(0).limitAmount()).isEqualByComparingTo("0");
    }
}
