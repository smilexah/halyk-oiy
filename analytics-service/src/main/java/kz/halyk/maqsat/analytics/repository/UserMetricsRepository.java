package kz.halyk.maqsat.analytics.repository;

import java.util.Optional;
import java.util.UUID;
import kz.halyk.maqsat.analytics.domain.UserMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMetricsRepository extends JpaRepository<UserMetrics, UUID> {

    Optional<UserMetrics> findByUserIdAndPeriod(String userId, String period);
}
