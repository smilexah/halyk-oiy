package kz.halyk.maqsat.analytics.repositories;

import java.math.BigDecimal;
import java.util.List;
import kz.halyk.maqsat.analytics.entities.Recommendation;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Disabled("H2 does not support JSONB List<String> mapping via @JdbcTypeCode — tested against real PG in integration tests")
class RecommendationRepositoryTest {

    @Autowired
    RecommendationRepository repo;

    @Test
    void roundtrip() {
        Recommendation r = new Recommendation();
        r.setUserId("u1");
        r.setPeriod("2026-05");
        r.setOfferId("offer-x");
        r.setPartner("ALSER");
        r.setScore(new BigDecimal("0.850"));
        r.setAudienceTags(List.of("saver", "apple-fan"));
        r.setRationale("test");
        repo.save(r);
        assertThat(repo.findByUserIdOrderByCreatedAtDesc("u1")).hasSize(1);
    }
}
