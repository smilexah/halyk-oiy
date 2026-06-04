package kz.halyk.maqsat.parsebudget.services.impl;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import java.util.UUID;
import kz.halyk.maqsat.parsebudget.client.BudgetClient;
import kz.halyk.maqsat.parsebudget.dto.req.BudgetPlanProposal;
import kz.halyk.maqsat.parsebudget.dto.req.ParsedPlanPayload;
import kz.halyk.maqsat.parsebudget.dto.req.ReplanRequest;
import kz.halyk.maqsat.parsebudget.dto.res.ActivePlanView;
import kz.halyk.maqsat.parsebudget.dto.res.ReplanResponse;
import kz.halyk.maqsat.parsebudget.exceptions.InvalidPlanException;
import kz.halyk.maqsat.parsebudget.services.ParseBudgetPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParseBudgetPlanServiceImpl implements ParseBudgetPlanService {

    private final BudgetClient budgetClient;
    private final MeterRegistry meterRegistry;

    @Override
    public ReplanResponse handle(ReplanRequest req) {
        try {
            validate(req.proposal());
        } catch (InvalidPlanException e) {
            meterRegistry.counter("maqsat.parse_budget.replan", "outcome", "invalid").increment();
            throw e;
        }
        Optional<ActivePlanView> current = budgetClient.getActive(req.userId());
        UUID supersededPlanId = current.map(ActivePlanView::planId).orElse(null);

        ParsedPlanPayload payload = new ParsedPlanPayload(
                "USER",
                req.userId(),
                req.period(),
                req.proposal().categories().stream()
                        .map(c -> new ParsedPlanPayload.PlannedCategory(c.name(), normalizeType(c.type()), c.limitAmount()))
                        .toList(),
                true,
                req.proposal().rationale());

        try {
            UUID newPlanId = budgetClient.replan(req.userId(), payload);
            meterRegistry.counter("maqsat.parse_budget.replan", "outcome", "ok").increment();
            log.info("Re-planned user={} period={} -> newPlanId={} (superseded={})",
                    req.userId(), req.period(), newPlanId, supersededPlanId);
            return new ReplanResponse(newPlanId, null, supersededPlanId);
        } catch (RuntimeException e) {
            meterRegistry.counter("maqsat.parse_budget.replan", "outcome", "error").increment();
            throw e;
        }
    }

    private void validate(BudgetPlanProposal p) {
        if (p == null || p.categories() == null || p.categories().isEmpty()) {
            throw new InvalidPlanException("Proposal has no categories");
        }
        for (var c : p.categories()) {
            if (c.name() == null || c.name().isBlank()) {
                throw new InvalidPlanException("Category with blank name");
            }
            if (c.limitAmount() == null || c.limitAmount().signum() < 0) {
                throw new InvalidPlanException("Invalid limit for " + c.name());
            }
            String t = normalizeType(c.type());
            if (!t.equals("MANDATORY") && !t.equals("DISCRETIONARY")) {
                throw new InvalidPlanException("Invalid type for " + c.name() + ": " + c.type());
            }
        }
    }

    private String normalizeType(String t) {
        return t == null ? "DISCRETIONARY" : t.toUpperCase();
    }
}
