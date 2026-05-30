package kz.halyk.maqsat.family.repository;

import java.util.UUID;
import kz.halyk.maqsat.family.domain.ChildLimit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildLimitRepository extends JpaRepository<ChildLimit, UUID> {
}
