package kz.halyk.maqsat.analytics.repository;

import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.analytics.domain.CategoryStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface CategoryStatRepository extends JpaRepository<CategoryStat, UUID> {

    List<CategoryStat> findByUserIdAndPeriod(String userId, String period);

    @Transactional
    void deleteByUserIdAndPeriod(String userId, String period);
}
