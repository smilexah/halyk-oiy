package kz.halyk.maqsat.common.event;

import java.time.Instant;
import java.util.List;

/** Published on topic {@link EventTopics#AI_SUMMARY_GENERATED} after the summary-llm-service
 *  produces a natural-language spending summary for a user's period. */
public record SummaryGenerated(
        String userId,
        String period,
        String language,
        String summaryText,
        List<String> highlights,
        List<String> suggestions,
        Instant generatedAt
) {
}
