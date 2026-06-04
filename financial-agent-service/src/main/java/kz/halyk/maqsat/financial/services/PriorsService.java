package kz.halyk.maqsat.financial.services;

import java.math.BigDecimal;
import java.util.Map;

public interface PriorsService {
    Map<String, BigDecimal> getPriorsFor(String segmentTag);
}
