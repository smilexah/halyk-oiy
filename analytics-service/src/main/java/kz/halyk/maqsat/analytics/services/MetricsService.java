package kz.halyk.maqsat.analytics.services;

import kz.halyk.maqsat.analytics.entities.UserMetrics;

public interface MetricsService {
    UserMetrics computeForUser(String userId, String period);
}
