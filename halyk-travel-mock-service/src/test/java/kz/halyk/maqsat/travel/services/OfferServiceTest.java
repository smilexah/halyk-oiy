package kz.halyk.maqsat.travel.services;

import kz.halyk.maqsat.travel.dto.req.ApplyDiscountRequest;
import kz.halyk.maqsat.travel.dto.res.ApplyDiscountResponse;
import kz.halyk.maqsat.travel.dto.res.TravelOfferDto;
import kz.halyk.maqsat.travel.entities.Booking;
import kz.halyk.maqsat.travel.entities.TravelOffer;
import kz.halyk.maqsat.travel.entities.enums.TravelOfferKind;
import kz.halyk.maqsat.travel.mappers.TravelOfferMapperImpl;
import kz.halyk.maqsat.travel.repositories.BookingRepository;
import kz.halyk.maqsat.travel.repositories.TravelOfferNativeRepository;
import kz.halyk.maqsat.travel.repositories.TravelOfferRepository;
import kz.halyk.maqsat.travel.services.impl.OfferServiceImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({OfferServiceImpl.class, TravelOfferNativeRepository.class, TravelOfferMapperImpl.class})
class OfferServiceTest {

    @Autowired
    private OfferService offerService;

    @Autowired
    private TravelOfferRepository offerRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void listAllReturnsSeeded() {
        TravelOffer o1 = buildOffer("Astana", TravelOfferKind.FLIGHT, new BigDecimal("100000"), new BigDecimal("10.00"));
        TravelOffer o2 = buildOffer("Dubai", TravelOfferKind.HOTEL, new BigDecimal("200000"), new BigDecimal("15.00"));
        offerRepository.save(o1);
        offerRepository.save(o2);

        List<TravelOfferDto> result = offerService.listAll();
        assertThat(result).hasSize(2);
    }

    @Test
    void applyCreatesBooking() {
        TravelOffer offer = buildOffer("Bali", TravelOfferKind.TOUR, new BigDecimal("100"), new BigDecimal("20.00"));
        offerRepository.save(offer);

        ApplyDiscountRequest req = new ApplyDiscountRequest("user-1", offer.getId());
        ApplyDiscountResponse resp = offerService.apply(req);

        assertThat(resp.finalPrice()).isEqualByComparingTo("80.00");
        assertThat(bookingRepository.count()).isEqualTo(1);
        Booking booking = bookingRepository.findAll().get(0);
        assertThat(booking.getUserId()).isEqualTo("user-1");
        assertThat(booking.getOfferId()).isEqualTo(offer.getId());
        assertThat(booking.getFinalPrice()).isEqualByComparingTo("80.00");
    }

    @Test
    @Disabled("requires Postgres jsonb ?| operator")
    void matchingAudience_requiresPostgres() {}

    private TravelOffer buildOffer(String destination, TravelOfferKind kind, BigDecimal basePrice, BigDecimal discountPct) {
        TravelOffer offer = new TravelOffer();
        offer.setKind(kind);
        offer.setDestination(destination);
        offer.setBasePrice(basePrice);
        offer.setDiscountPct(discountPct);
        offer.setAudienceTags(List.of("test-tag"));
        offer.setValidUntil(Instant.now().plusSeconds(86400));
        return offer;
    }
}
