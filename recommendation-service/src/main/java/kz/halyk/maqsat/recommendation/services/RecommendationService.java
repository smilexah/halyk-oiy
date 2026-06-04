package kz.halyk.maqsat.recommendation.services;

import kz.halyk.maqsat.recommendation.dto.res.RecommendationResult;

public interface RecommendationService {
    RecommendationResult recommend(String userId, String period);
}
