package kz.halyk.maqsat.transaction.services;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.transaction.dto.req.TransactionRequest;
import kz.halyk.maqsat.transaction.dto.res.TransactionResponse;
import kz.halyk.maqsat.transaction.entities.Transaction;

public interface TransactionService {
    Transaction ingest(String userId, String bearer, TransactionRequest request);
    void approve(UUID transactionId);
    List<Transaction> listForUser(String userId);
    List<TransactionResponse> findSince(Instant since);
}
