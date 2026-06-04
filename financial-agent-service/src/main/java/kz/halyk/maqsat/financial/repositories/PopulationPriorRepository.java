package kz.halyk.maqsat.financial.repositories;

import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.financial.entities.PopulationPrior;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PopulationPriorRepository extends JpaRepository<PopulationPrior, UUID> {

    List<PopulationPrior> findBySegmentTag(String segmentTag);
}
