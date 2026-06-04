package kz.halyk.maqsat.goals.repositories;

import java.util.UUID;
import kz.halyk.maqsat.goals.entities.VirtualAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VirtualAccountRepository extends JpaRepository<VirtualAccount, UUID> {}
