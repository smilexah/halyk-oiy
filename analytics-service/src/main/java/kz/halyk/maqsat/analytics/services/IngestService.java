package kz.halyk.maqsat.analytics.services;

import kz.halyk.maqsat.common.event.TransactionCategorized;

public interface IngestService {
    void handle(TransactionCategorized event);
}
