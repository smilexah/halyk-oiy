package kz.halyk.maqsat.goals.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.common.security.CurrentUser;
import kz.halyk.maqsat.goals.dto.ContributeRequest;
import kz.halyk.maqsat.goals.dto.CreateGoalRequest;
import kz.halyk.maqsat.goals.dto.GoalResponse;
import kz.halyk.maqsat.goals.service.GoalService;
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
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService service;

    @PostMapping
    public ResponseEntity<GoalResponse> create(@Valid @RequestBody CreateGoalRequest request) {
        String userId = CurrentUser.current().userId();
        GoalResponse body = GoalResponse.from(service.createGoal(userId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public List<GoalResponse> list() {
        String userId = CurrentUser.current().userId();
        return service.listForUser(userId).stream().map(GoalResponse::from).toList();
    }

    @PostMapping("/{id}/contribute")
    public GoalResponse contribute(@PathVariable UUID id, @Valid @RequestBody ContributeRequest request) {
        String userId = CurrentUser.current().userId();
        return GoalResponse.from(service.contribute(userId, id, request.amount()));
    }
}