package kz.halyk.maqsat.alser.entities;

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
@Table(name = "applied_discount")
@Getter
@Setter
@NoArgsConstructor
public class AppliedDiscount {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "offer_id", nullable = false)
    private UUID offerId;

    @Column(name = "final_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal finalPrice;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @PrePersist
    void prePersist() {
        if (appliedAt == null) {
            appliedAt = Instant.now();
        }
    }
}
