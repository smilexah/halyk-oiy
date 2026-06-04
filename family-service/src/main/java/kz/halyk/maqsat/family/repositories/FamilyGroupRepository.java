package kz.halyk.maqsat.family.repositories;

import java.util.UUID;
import kz.halyk.maqsat.family.entities.FamilyGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyGroupRepository extends JpaRepository<FamilyGroup, UUID> {}
