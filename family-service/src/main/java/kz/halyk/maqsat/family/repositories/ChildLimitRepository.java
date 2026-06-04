package kz.halyk.maqsat.family.repositories;

import java.util.UUID;
import kz.halyk.maqsat.family.entities.ChildLimit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildLimitRepository extends JpaRepository<ChildLimit, UUID> {}
