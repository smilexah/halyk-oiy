package kz.halyk.maqsat.analytics.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "txn_fact")
@Getter
@Setter
@NoArgsConstructor
public class TxnFact {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private UUID transactionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 8)
    private String mcc;

    @Column(name = "category_name", nullable = false, length = 128)
    private String categoryName;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    @Column(length = 8)
    private String direction;

    @Column(name = "operation_type", length = 16)
    private String operationType;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 512)
    private String details;

    @Column(name = "balance_after", precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @PrePersist
    public void prePersist() {
        if (ingestedAt == null) {
            ingestedAt = Instant.now();
        }
        if (currency == null) {
            currency = "KZT";
        }
    }
}
