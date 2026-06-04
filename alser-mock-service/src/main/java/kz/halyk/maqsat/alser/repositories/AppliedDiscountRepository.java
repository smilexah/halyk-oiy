package kz.halyk.maqsat.alser.repositories;

import java.util.UUID;
import kz.halyk.maqsat.alser.entities.AppliedDiscount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppliedDiscountRepository extends JpaRepository<AppliedDiscount, UUID> {}
