package kz.halyk.maqsat.analytics.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "drift_report")
@Getter
@Setter
@NoArgsConstructor
public class DriftReport {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(nullable = false, length = 7)
    private String period;

    @Column(nullable = false)
    private boolean matches;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "drift_by_category", nullable = false)
    private Map<String, BigDecimal> driftByCategory;

    @Column(name = "recommend_adjustment", nullable = false)
    private boolean recommendAdjustment;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    @PrePersist
    public void prePersist() {
        if (computedAt == null) {
            computedAt = Instant.now();
        }
    }
}
