package kz.halyk.maqsat.analytics.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "category_stat", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "period", "category_name"}))
@Getter
@Setter
@NoArgsConstructor
public class CategoryStat {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(nullable = false, length = 7)
    private String period;

    @Column(name = "category_name", nullable = false, length = 128)
    private String categoryName;

    @Column(name = "txn_count", nullable = false)
    private int txnCount;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "avg_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal avgAmount;

    @Column(name = "median_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal medianAmount;
}
