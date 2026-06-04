package kz.halyk.maqsat.financial.entities;

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
@Table(
    name = "population_prior",
    uniqueConstraints = @UniqueConstraint(columnNames = {"segment_tag", "category_name"})
)
@Getter
@Setter
@NoArgsConstructor
public class PopulationPrior {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "segment_tag", nullable = false, length = 64)
    private String segmentTag;

    @Column(name = "category_name", nullable = false, length = 128)
    private String categoryName;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal ratio;

    @Column(nullable = false, length = 128)
    private String source;
}
