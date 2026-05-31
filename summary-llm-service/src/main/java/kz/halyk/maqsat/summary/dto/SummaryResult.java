package kz.halyk.maqsat.summary.dto;

import java.util.List;

public record SummaryResult(
        String language,
        String summaryText,
        List<String> highlights,
        List<String> suggestions
) {}
