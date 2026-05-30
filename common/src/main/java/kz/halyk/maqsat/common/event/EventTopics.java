package kz.halyk.maqsat.common.event;

/** Kafka topic names shared across services. */
public final class EventTopics {

    public static final String TRANSACTION_CATEGORIZED = "transaction.categorized";
    public static final String TRANSACTION_LIMIT_EXCEEDED = "transaction.limit-exceeded";
    public static final String FAMILY_LIMIT_OVERRIDE_APPROVED = "family.limit-override-approved";

    private EventTopics() {
    }
}