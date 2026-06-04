package kz.halyk.maqsat.transaction.controllers;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import kz.halyk.maqsat.common.security.CurrentUser;
import kz.halyk.maqsat.transaction.dto.req.TransactionRequest;
import kz.halyk.maqsat.transaction.dto.res.TransactionResponse;
import kz.halyk.maqsat.transaction.mappers.TransactionMapper;
import kz.halyk.maqsat.transaction.services.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;
    private final TransactionMapper transactionMapper;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                      @Valid @RequestBody TransactionRequest request) {
        TransactionResponse body = transactionMapper.toResponse(
                service.ingest(CurrentUser.current().userId(), jwt.getTokenValue(), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public List<TransactionResponse> list() {
        return service.listForUser(CurrentUser.current().userId()).stream()
                .map(transactionMapper::toResponse).toList();
    }

    @GetMapping("/internal/since")
    public List<TransactionResponse> since(@RequestParam Instant since) {
        return service.findSince(since);
    }
}
