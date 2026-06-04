package kz.halyk.maqsat.summary.services;

import kz.halyk.maqsat.summary.client.ActivePlanView;
import kz.halyk.maqsat.summary.client.UserMetricsDto;
import kz.halyk.maqsat.summary.dto.res.SummaryResult;

public interface SummaryLlmService {
    SummaryResult summarise(String userId, String period, String language,
                            UserMetricsDto metrics, ActivePlanView plan);
}
