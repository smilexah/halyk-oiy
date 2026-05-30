package kz.halyk.maqsat.family.repository;

import java.util.UUID;
import kz.halyk.maqsat.family.domain.FamilyGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyGroupRepository extends JpaRepository<FamilyGroup, UUID> {
}
