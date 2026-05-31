package kz.halyk.maqsat.financial.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import kz.halyk.maqsat.financial.repository.PopulationPriorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PriorsService {

    private final PopulationPriorRepository repo;

    public Map<String, BigDecimal> getPriorsFor(String segmentTag) {
        return repo.findBySegmentTag(segmentTag).stream()
                .collect(Collectors.toMap(
                        p -> p.getCategoryName(),
                        p -> p.getRatio(),
                        (a, b) -> a,
                        LinkedHashMap::new));
    }
}
