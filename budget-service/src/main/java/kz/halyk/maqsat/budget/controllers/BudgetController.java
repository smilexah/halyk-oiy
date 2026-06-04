package kz.halyk.maqsat.budget.controllers;

import jakarta.validation.Valid;
import java.util.UUID;
import kz.halyk.maqsat.budget.dto.req.CreatePlanRequest;
import kz.halyk.maqsat.budget.dto.req.ParsedPlanPayload;
import kz.halyk.maqsat.budget.dto.res.ActivePlanView;
import kz.halyk.maqsat.budget.dto.res.DashboardResponse;
import kz.halyk.maqsat.budget.entities.BudgetPlan;
import kz.halyk.maqsat.budget.services.BudgetService;
import kz.halyk.maqsat.common.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService service;

    @PostMapping("/plan")
    public ResponseEntity<UUID> createPlan(@Valid @RequestBody CreatePlanRequest request) {
        String userId = CurrentUser.current().userId();
        BudgetPlan plan = service.createPlan(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(plan.getId());
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        String userId = CurrentUser.current().userId();
        return service.dashboard(userId);
    }

    @GetMapping("/internal/active/{userId}")
    public ResponseEntity<ActivePlanView> internalActivePlan(@PathVariable String userId) {
        ActivePlanView view = service.getActivePlan(userId);
        if (view == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(view);
    }

    @PostMapping("/internal/replan/{userId}")
    public ResponseEntity<UUID> replan(@PathVariable String userId,
                                       @Valid @RequestBody ParsedPlanPayload payload) {
        BudgetPlan p = service.replan(userId, payload);
        return ResponseEntity.status(HttpStatus.CREATED).body(p.getId());
    }
}
