package kz.halyk.maqsat.goals.repository;

import java.util.UUID;
import kz.halyk.maqsat.goals.domain.VirtualAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VirtualAccountRepository extends JpaRepository<VirtualAccount, UUID> {
}