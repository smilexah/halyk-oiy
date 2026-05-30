package kz.halyk.maqsat.transaction.service;

import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Maps a transaction to a budget category. First by MCC, then by a merchant-name heuristic,
 * finally falling back to "Прочее". Category names match the budget categories exactly.
 */
@Component
public class CategorizationEngine {

    public static final String FALLBACK = "Прочее";

    private static final Map<String, String> BY_MCC = Map.ofEntries(
            Map.entry("5411", "Продукты"),
            Map.entry("5412", "Продукты"),
            Map.entry("5499", "Продукты"),
            Map.entry("5541", "Транспорт"),
            Map.entry("5542", "Транспорт"),
            Map.entry("4111", "Такси"),
            Map.entry("4121", "Такси"),
            Map.entry("5812", "Рестораны"),
            Map.entry("5814", "Рестораны"),
            Map.entry("4900", "Коммуналка"),
            Map.entry("7997", "Развлечения"),
            Map.entry("7991", "Развлечения")
    );

    public String categorize(String mcc, String merchant) {
        if (mcc != null) {
            String byMcc = BY_MCC.get(mcc.trim());
            if (byMcc != null) {
                return byMcc;
            }
        }
        String byMerchant = byMerchant(merchant);
        return byMerchant != null ? byMerchant : FALLBACK;
    }

    private String byMerchant(String merchant) {
        if (merchant == null || merchant.isBlank()) {
            return null;
        }
        String m = merchant.toLowerCase();
        if (containsAny(m, "magnum", "small", "galmart", "продукт", "market", "супермаркет")) {
            return "Продукты";
        }
        if (containsAny(m, "yandex.go", "yandex go", "indriver", "такси", "taxi")) {
            return "Такси";
        }
        if (containsAny(m, "kfc", "burger", "coffee", "ресторан", "кафе", "cafe", "bar")) {
            return "Рестораны";
        }
        if (containsAny(m, "bus", "metro", "metropolitan", "автобус", "fuel", "petrol", "азс")) {
            return "Транспорт";
        }
        if (containsAny(m, "kazpost", "kommun", "коммунал", "energo", "water", "gas")) {
            return "Коммуналка";
        }
        if (containsAny(m, "cinema", "kino", "theatre", "steam", "game", "развлеч")) {
            return "Развлечения";
        }
        return null;
    }

    private boolean containsAny(String haystack, String... needles) {
        for (String n : needles) {
            if (haystack.contains(n)) {
                return true;
            }
        }
        return false;
    }
}