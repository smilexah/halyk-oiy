package kz.halyk.maqsat.parsebudget.controller;

import jakarta.validation.Valid;
import kz.halyk.maqsat.parsebudget.dto.ReplanRequest;
import kz.halyk.maqsat.parsebudget.dto.ReplanResponse;
import kz.halyk.maqsat.parsebudget.service.ParseBudgetPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parse-budget")
@RequiredArgsConstructor
public class ParseBudgetPlanController {

    private final ParseBudgetPlanService service;

    @PostMapping("/replan")
    public ResponseEntity<ReplanResponse> replan(@Valid @RequestBody ReplanRequest req) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.handle(req));
    }
}
