package kz.halyk.maqsat.alser.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import kz.halyk.maqsat.alser.domain.DeviceOffer;
import kz.halyk.maqsat.alser.dto.ApplyDiscountRequest;
import kz.halyk.maqsat.alser.dto.ApplyDiscountResponse;
import kz.halyk.maqsat.alser.dto.DeviceOfferDto;
import kz.halyk.maqsat.alser.repository.AppliedDiscountRepository;
import kz.halyk.maqsat.alser.repository.DeviceOfferRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(OfferService.class)
class OfferServiceTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    OfferService offerService;

    @Autowired
    DeviceOfferRepository offerRepo;

    @Autowired
    AppliedDiscountRepository appliedRepo;

    @Test
    void listAllReturnsSeeded() {
        em.persistAndFlush(buildOffer("SKU-1", "Device One", new BigDecimal("100000"), new BigDecimal("10.00")));
        em.persistAndFlush(buildOffer("SKU-2", "Device Two", new BigDecimal("200000"), new BigDecimal("15.00")));

        List<DeviceOfferDto> result = offerService.listAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(DeviceOfferDto::sku).containsExactlyInAnyOrder("SKU-1", "SKU-2");
    }

    @Test
    void applyComputesFinalPrice() {
        DeviceOffer offer = buildOffer("SKU-DISC", "Discount Device", new BigDecimal("100.00"), new BigDecimal("10.00"));
        em.persistAndFlush(offer);

        ApplyDiscountRequest req = new ApplyDiscountRequest("user-test-1", offer.getId());
        ApplyDiscountResponse response = offerService.apply(req);

        assertThat(response.finalPrice()).isEqualByComparingTo(new BigDecimal("90.00"));
        assertThat(response.userId()).isEqualTo("user-test-1");
        assertThat(response.offerId()).isEqualTo(offer.getId());
        assertThat(response.appliedAt()).isNotNull();
    }

    @Test
    @Disabled("requires Postgres jsonb ?| operator — the native query uses GIN index and ?| which H2 does not support")
    void matchingAudienceReturnsFilteredOffers() {
        // This test requires a real Postgres instance.
        // Production code: DeviceOfferRepository.findActiveMatchingAudience uses
        //   SELECT * FROM device_offer WHERE audience_tags ?| cast(:tags as text[]) AND valid_until > now()
        // which relies on the Postgres jsonb containment operator and GIN index.
    }

    private DeviceOffer buildOffer(String sku, String name, BigDecimal basePrice, BigDecimal discountPct) {
        DeviceOffer offer = new DeviceOffer();
        offer.setSku(sku);
        offer.setName(name);
        offer.setBasePrice(basePrice);
        offer.setDiscountPct(discountPct);
        offer.setAudienceTags(List.of("test-tag"));
        offer.setValidUntil(Instant.now().plus(30, ChronoUnit.DAYS));
        return offer;
    }
}
