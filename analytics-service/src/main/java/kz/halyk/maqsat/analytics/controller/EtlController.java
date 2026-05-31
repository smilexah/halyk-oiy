package kz.halyk.maqsat.analytics.controller;

import kz.halyk.maqsat.analytics.service.BatchEtlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics/etl")
@RequiredArgsConstructor
public class EtlController {

    private final BatchEtlJob job;

    /**
     * Manually triggers the batch ETL job. Intended for demos and integration tests.
     * Returns 202 Accepted immediately after the job completes.
     */
    @PostMapping("/run")
    public ResponseEntity<Void> runEtl() {
        job.run();
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
