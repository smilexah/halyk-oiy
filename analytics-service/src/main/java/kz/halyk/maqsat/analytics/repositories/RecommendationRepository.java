package kz.halyk.maqsat.analytics.repositories;

import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.analytics.entities.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    List<Recommendation> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Recommendation> findByUserIdAndPeriodOrderByCreatedAtDesc(String userId, String period);

    void deleteByUserIdAndPeriod(String userId, String period);
}
