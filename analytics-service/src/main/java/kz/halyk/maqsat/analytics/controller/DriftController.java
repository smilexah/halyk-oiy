package kz.halyk.maqsat.analytics.controller;

import kz.halyk.maqsat.analytics.dto.DriftReportResponse;
import kz.halyk.maqsat.analytics.repository.DriftReportRepository;
import kz.halyk.maqsat.analytics.service.DriftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/analytics/drift")
@RequiredArgsConstructor
public class DriftController {

    private final DriftService driftService;
    private final DriftReportRepository reports;

    @GetMapping("/{userId}/{period}")
    public DriftReportResponse get(@PathVariable String userId, @PathVariable String period) {
        return reports.findTopByUserIdAndPeriodOrderByComputedAtDesc(userId, period)
                .map(DriftReportResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No drift report for userId=" + userId + " period=" + period));
    }

    @PostMapping("/{userId}/{period}/recompute")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DriftReportResponse recompute(@PathVariable String userId, @PathVariable String period) {
        return DriftReportResponse.from(driftService.detect(userId, period));
    }
}
