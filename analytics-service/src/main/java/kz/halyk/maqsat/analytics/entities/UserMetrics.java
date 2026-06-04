package kz.halyk.maqsat.analytics.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "user_metrics", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "period"}))
@Getter
@Setter
@NoArgsConstructor
public class UserMetrics {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(nullable = false, length = 7)
    private String period;

    @Column(name = "income_estimate", precision = 15, scale = 2)
    private BigDecimal incomeEstimate;

    @Column(name = "total_spent", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalSpent;

    @Column(name = "avg_transaction", precision = 15, scale = 2)
    private BigDecimal avgTransaction;

    @Column(name = "median_transaction", precision = 15, scale = 2)
    private BigDecimal medianTransaction;

    @Column(precision = 6, scale = 4)
    private BigDecimal volatility;

    @Column(name = "savings_rate", precision = 6, scale = 4)
    private BigDecimal savingsRate;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    @PrePersist
    public void prePersist() {
        if (computedAt == null) {
            computedAt = Instant.now();
        }
    }
}
