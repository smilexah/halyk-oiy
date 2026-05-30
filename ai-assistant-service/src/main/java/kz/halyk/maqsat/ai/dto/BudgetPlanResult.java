package kz.halyk.maqsat.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BudgetPlanResult(
        List<PlannedCategory> categories,
        String rationale
) {
}
