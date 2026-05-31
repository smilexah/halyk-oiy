package kz.halyk.maqsat.financial.repository;

import java.util.List;
import kz.halyk.maqsat.financial.domain.PopulationPrior;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PopulationPriorRepository extends JpaRepository<PopulationPrior, UUID> {

    List<PopulationPrior> findBySegmentTag(String segmentTag);
}
