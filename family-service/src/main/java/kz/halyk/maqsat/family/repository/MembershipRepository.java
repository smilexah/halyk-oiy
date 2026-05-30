package kz.halyk.maqsat.family.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kz.halyk.maqsat.family.domain.Membership;
import kz.halyk.maqsat.family.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    List<Membership> findByUserId(String userId);

    Optional<Membership> findFirstByUserIdAndRole(String userId, Role role);
}
