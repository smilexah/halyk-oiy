package kz.halyk.maqsat.analytics.services;

import kz.halyk.maqsat.analytics.entities.DriftReport;

public interface DriftService {
    DriftReport detect(String userId, String period);
}
