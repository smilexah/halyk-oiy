package kz.halyk.maqsat.alser.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import kz.halyk.maqsat.alser.dto.req.ApplyDiscountRequest;
import kz.halyk.maqsat.alser.dto.res.ApplyDiscountResponse;
import kz.halyk.maqsat.alser.dto.res.DeviceOfferDto;
import kz.halyk.maqsat.alser.entities.DeviceOffer;
import kz.halyk.maqsat.alser.mappers.DeviceOfferMapperImpl;
import kz.halyk.maqsat.alser.repositories.AppliedDiscountRepository;
import kz.halyk.maqsat.alser.repositories.DeviceOfferRepository;
import kz.halyk.maqsat.alser.services.impl.OfferServiceImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({OfferServiceImpl.class, DeviceOfferMapperImpl.class})
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
    @Disabled("requires Postgres jsonb ?| operator")
    void matchingAudienceReturnsFilteredOffers() {}

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
