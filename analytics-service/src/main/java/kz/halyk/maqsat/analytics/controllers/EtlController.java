package kz.halyk.maqsat.analytics.controllers;

import kz.halyk.maqsat.analytics.schedulers.BatchEtlJob;
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

    @PostMapping("/run")
    public ResponseEntity<Void> runEtl() {
        job.run();
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
