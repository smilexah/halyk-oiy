package kz.halyk.maqsat.financial.services.impl;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import kz.halyk.maqsat.financial.repositories.PopulationPriorRepository;
import kz.halyk.maqsat.financial.services.PriorsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PriorsServiceImpl implements PriorsService {

    private final PopulationPriorRepository repo;

    @Override
    public Map<String, BigDecimal> getPriorsFor(String segmentTag) {
        return repo.findBySegmentTag(segmentTag).stream()
                .collect(Collectors.toMap(
                        p -> p.getCategoryName(),
                        p -> p.getRatio(),
                        (a, b) -> a,
                        LinkedHashMap::new));
    }
}
