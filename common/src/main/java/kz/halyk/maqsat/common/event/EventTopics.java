package kz.halyk.maqsat.common.event;

/** Kafka topic names shared across services. */
public final class EventTopics {

    public static final String TRANSACTION_CATEGORIZED = "transaction.categorized";
    public static final String TRANSACTION_LIMIT_EXCEEDED = "transaction.limit-exceeded";
    public static final String FAMILY_LIMIT_OVERRIDE_APPROVED = "family.limit-override-approved";

    public static final String ANALYTICS_METRICS_COMPUTED = "analytics.metrics-computed";
    public static final String ANALYTICS_PLAN_DRIFT_DETECTED = "analytics.plan-drift-detected";
    public static final String AI_SUMMARY_GENERATED = "ai.summary-generated";
    public static final String AI_RECOMMENDATION_READY = "ai.recommendation-ready";

    private EventTopics() {
    }
}