package kz.halyk.maqsat.alser.repository;

import java.util.UUID;
import kz.halyk.maqsat.alser.domain.AppliedDiscount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppliedDiscountRepository extends JpaRepository<AppliedDiscount, UUID> {
}
