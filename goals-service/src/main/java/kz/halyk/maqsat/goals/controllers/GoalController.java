package kz.halyk.maqsat.goals.controllers;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.common.security.CurrentUser;
import kz.halyk.maqsat.goals.dto.req.ContributeRequest;
import kz.halyk.maqsat.goals.dto.req.CreateGoalRequest;
import kz.halyk.maqsat.goals.dto.res.GoalResponse;
import kz.halyk.maqsat.goals.mappers.GoalMapper;
import kz.halyk.maqsat.goals.services.GoalService;
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
    private final GoalMapper goalMapper;

    @PostMapping
    public ResponseEntity<GoalResponse> create(@Valid @RequestBody CreateGoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(goalMapper.toResponse(service.createGoal(CurrentUser.current().userId(), request)));
    }

    @GetMapping
    public List<GoalResponse> list() {
        return service.listForUser(CurrentUser.current().userId()).stream().map(goalMapper::toResponse).toList();
    }

    @GetMapping("/internal/{userId}")
    public List<GoalResponse> internal(@PathVariable String userId) {
        return service.internalListForUser(userId).stream().map(goalMapper::toResponse).toList();
    }

    @PostMapping("/{id}/contribute")
    public GoalResponse contribute(@PathVariable UUID id, @Valid @RequestBody ContributeRequest request) {
        return goalMapper.toResponse(service.contribute(CurrentUser.current().userId(), id, request.amount()));
    }
}
