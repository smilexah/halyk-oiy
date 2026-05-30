package kz.halyk.maqsat.transaction.controller;

import jakarta.validation.Valid;
import java.util.List;
import kz.halyk.maqsat.common.security.CurrentUser;
import kz.halyk.maqsat.transaction.dto.TransactionRequest;
import kz.halyk.maqsat.transaction.dto.TransactionResponse;
import kz.halyk.maqsat.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                      @Valid @RequestBody TransactionRequest request) {
        String userId = CurrentUser.current().userId();
        TransactionResponse body = TransactionResponse.from(service.ingest(userId, jwt.getTokenValue(), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public List<TransactionResponse> list() {
        String userId = CurrentUser.current().userId();
        return service.listForUser(userId).stream().map(TransactionResponse::from).toList();
    }
}