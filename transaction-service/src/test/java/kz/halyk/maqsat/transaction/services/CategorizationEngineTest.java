package kz.halyk.maqsat.transaction.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CategorizationEngineTest {

    private final CategorizationEngine engine = new CategorizationEngine();

    @Test
    void categorizesByMcc() {
        assertThat(engine.categorize("5411", null)).isEqualTo("Продукты");
        assertThat(engine.categorize("5541", null)).isEqualTo("Транспорт");
        assertThat(engine.categorize("4111", null)).isEqualTo("Такси");
        assertThat(engine.categorize("5812", null)).isEqualTo("Рестораны");
        assertThat(engine.categorize("4900", null)).isEqualTo("Коммуналка");
        assertThat(engine.categorize("7997", null)).isEqualTo("Развлечения");
    }

    @Test
    void fallsBackToMerchantHeuristic() {
        assertThat(engine.categorize(null, "MAGNUM Almaty")).isEqualTo("Продукты");
        assertThat(engine.categorize(null, "Yandex.Go")).isEqualTo("Такси");
    }

    @Test
    void fallsBackToProchee() {
        assertThat(engine.categorize(null, null)).isEqualTo("Прочее");
        assertThat(engine.categorize("9999", "Unknown Merchant XYZ")).isEqualTo("Прочее");
    }
}
