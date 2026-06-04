package kz.halyk.maqsat.analytics.repositories;

import java.util.Optional;
import java.util.UUID;
import kz.halyk.maqsat.analytics.entities.DriftReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriftReportRepository extends JpaRepository<DriftReport, UUID> {

    Optional<DriftReport> findTopByUserIdAndPeriodOrderByComputedAtDesc(String userId, String period);
}
