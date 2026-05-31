package kz.halyk.maqsat.transaction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "txn")
@Getter
@Setter
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column
    private String merchant;

    @Column
    private String mcc;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    /** Whether the transaction debits or credits the account. Defaults to DEBIT. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;

    /** High-level operation type used for analytics segmentation. Defaults to PURCHASE. */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;

    /** ISO 4217 currency code. Defaults to KZT. */
    @Column(nullable = false, length = 3)
    private String currency;

    /** Free-text details or reference (e.g. transfer description). Nullable. */
    @Column(length = 512)
    private String details;

    /** Account balance after the transaction was applied. Nullable. */
    @Column(name = "balance_after", precision = 15, scale = 2)
    private BigDecimal balanceAfter;
}