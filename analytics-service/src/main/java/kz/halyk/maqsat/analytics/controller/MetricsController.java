package kz.halyk.maqsat.analytics.controller;

import kz.halyk.maqsat.analytics.dto.UserMetricsResponse;
import kz.halyk.maqsat.analytics.repository.CategoryStatRepository;
import kz.halyk.maqsat.analytics.repository.RecurringDebitRepository;
import kz.halyk.maqsat.analytics.repository.UserMetricsRepository;
import kz.halyk.maqsat.analytics.service.MetricsService;
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
@RequestMapping("/api/analytics/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;
    private final UserMetricsRepository metricsRepo;
    private final CategoryStatRepository catRepo;
    private final RecurringDebitRepository recRepo;

    @GetMapping("/{userId}/{period}")
    public UserMetricsResponse get(@PathVariable String userId, @PathVariable String period) {
        var m = metricsRepo.findByUserIdAndPeriod(userId, period)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No metrics for userId=" + userId + " period=" + period));
        return UserMetricsResponse.from(m,
                catRepo.findByUserIdAndPeriod(userId, period),
                recRepo.findByUserId(userId));
    }

    @PostMapping("/{userId}/{period}/recompute")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public UserMetricsResponse recompute(@PathVariable String userId, @PathVariable String period) {
        var m = metricsService.computeForUser(userId, period);
        return UserMetricsResponse.from(m,
                catRepo.findByUserIdAndPeriod(userId, period),
                recRepo.findByUserId(userId));
    }
}
