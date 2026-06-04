package kz.halyk.maqsat.financial.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;
import kz.halyk.maqsat.financial.entities.PopulationPrior;
import kz.halyk.maqsat.financial.repositories.PopulationPriorRepository;
import kz.halyk.maqsat.financial.services.impl.PriorsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(PriorsServiceImpl.class)
@ActiveProfiles("test")
class PriorsServiceTest {

    @Autowired
    private PriorsService priorsService;

    @Autowired
    private TestEntityManager em;

    @BeforeEach
    void seed() {
        String[] categories = {"Продукты", "Коммуналка", "Транспорт", "Такси", "Рестораны", "Развлечения", "Подписки", "Прочее"};
        double[] ratios = {0.25, 0.12, 0.08, 0.05, 0.09, 0.07, 0.03, 0.20};
        for (int i = 0; i < categories.length; i++) {
            PopulationPrior p = new PopulationPrior();
            p.setSegmentTag("default");
            p.setCategoryName(categories[i]);
            p.setRatio(BigDecimal.valueOf(ratios[i]));
            p.setSource("test");
            em.persist(p);
        }
        em.flush();
    }

    @Test
    void returnsRatiosForDefaultSegment() {
        Map<String, BigDecimal> priors = priorsService.getPriorsFor("default");
        assertThat(priors).hasSizeGreaterThanOrEqualTo(8);
        BigDecimal sum = priors.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isGreaterThan(new BigDecimal("0.8"));
    }

    @Test
    void returnsEmptyForUnknownSegment() {
        assertThat(priorsService.getPriorsFor("nonexistent")).isEmpty();
    }
}
