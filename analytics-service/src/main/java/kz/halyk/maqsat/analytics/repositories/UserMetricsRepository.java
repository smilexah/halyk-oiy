package kz.halyk.maqsat.analytics.repositories;

import java.util.Optional;
import java.util.UUID;
import kz.halyk.maqsat.analytics.entities.UserMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMetricsRepository extends JpaRepository<UserMetrics, UUID> {

    Optional<UserMetrics> findByUserIdAndPeriod(String userId, String period);
}
