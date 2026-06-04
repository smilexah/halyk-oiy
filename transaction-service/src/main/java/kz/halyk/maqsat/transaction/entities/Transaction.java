package kz.halyk.maqsat.transaction.entities;

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
import kz.halyk.maqsat.transaction.entities.enums.Direction;
import kz.halyk.maqsat.transaction.entities.enums.OperationType;
import kz.halyk.maqsat.transaction.entities.enums.TransactionStatus;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 512)
    private String details;

    @Column(name = "balance_after", precision = 15, scale = 2)
    private BigDecimal balanceAfter;
}
