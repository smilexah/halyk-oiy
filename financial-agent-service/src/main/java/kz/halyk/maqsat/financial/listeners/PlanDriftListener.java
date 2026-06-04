package kz.halyk.maqsat.financial.listeners;

import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import kz.halyk.maqsat.common.event.EventTopics;
import kz.halyk.maqsat.common.event.PlanDriftDetected;
import kz.halyk.maqsat.financial.client.AnalyticsClient;
import kz.halyk.maqsat.financial.client.ParseBudgetPlanClient;
import kz.halyk.maqsat.financial.dto.req.ReplanRequest;
import kz.halyk.maqsat.financial.dto.res.BudgetPlanProposal;
import kz.halyk.maqsat.financial.dto.res.ReplanResponse;
import kz.halyk.maqsat.financial.dto.res.UserMetricsDto;
import kz.halyk.maqsat.financial.services.FinancialAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlanDriftListener {

    private final AnalyticsClient analytics;
    private final FinancialAgentService agent;
    private final ParseBudgetPlanClient parser;
    private final MeterRegistry meterRegistry;

    @KafkaListener(topics = EventTopics.ANALYTICS_PLAN_DRIFT_DETECTED, groupId = "financial-agent-service")
    public void onDrift(PlanDriftDetected e) {
        log.info("Received PlanDriftDetected for user={} period={}", e.userId(), e.period());
        UserMetricsDto metrics = analytics.fetchMetrics(e.userId(), e.period());
        if (metrics == null) {
            log.warn("No metrics found for user={} period={}, skipping replan", e.userId(), e.period());
            return;
        }
        BudgetPlanProposal proposal = agent.propose(
                e.userId(), e.period(), "default",
                toMetricsMap(metrics), e.driftByCategory());
        ReplanResponse response = parser.replan(new ReplanRequest(e.userId(), e.period(), proposal, e.driftByCategory()));
        if (response != null) {
            meterRegistry.counter("maqsat.replans", "trigger", "drift").increment();
            log.info("Replan submitted for user={} period={} newPlanId={}", e.userId(), e.period(), response.newPlanId());
        }
    }

    private Map<String, BigDecimal> toMetricsMap(UserMetricsDto m) {
        var out = new LinkedHashMap<String, BigDecimal>();
        if (m.incomeEstimate() != null) out.put("incomeEstimate", m.incomeEstimate());
        if (m.totalSpent() != null) out.put("totalSpent", m.totalSpent());
        if (m.savingsRate() != null) out.put("savingsRate", m.savingsRate());
        if (m.volatility() != null) out.put("volatility", m.volatility());
        return out;
    }
}
