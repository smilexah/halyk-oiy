package kz.halyk.maqsat.analytics.repository;

import java.util.Optional;
import java.util.UUID;
import kz.halyk.maqsat.analytics.domain.DriftReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriftReportRepository extends JpaRepository<DriftReport, UUID> {

    Optional<DriftReport> findTopByUserIdAndPeriodOrderByComputedAtDesc(String userId, String period);
}
