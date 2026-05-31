package kz.halyk.maqsat.travel.service;

import kz.halyk.maqsat.travel.domain.Booking;
import kz.halyk.maqsat.travel.domain.TravelOffer;
import kz.halyk.maqsat.travel.domain.TravelOfferKind;
import kz.halyk.maqsat.travel.dto.ApplyDiscountRequest;
import kz.halyk.maqsat.travel.dto.ApplyDiscountResponse;
import kz.halyk.maqsat.travel.dto.TravelOfferDto;
import kz.halyk.maqsat.travel.repository.BookingRepository;
import kz.halyk.maqsat.travel.repository.TravelOfferNativeRepository;
import kz.halyk.maqsat.travel.repository.TravelOfferRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({OfferService.class, TravelOfferNativeRepository.class})
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
    void matchingAudience_requiresPostgres() {
        // This test requires the native Postgres ?| operator and cannot run on H2.
    }

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
