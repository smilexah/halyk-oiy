package kz.halyk.maqsat.travel.service;

import kz.halyk.maqsat.travel.domain.Booking;
import kz.halyk.maqsat.travel.domain.TravelOffer;
import kz.halyk.maqsat.travel.dto.ApplyDiscountRequest;
import kz.halyk.maqsat.travel.dto.ApplyDiscountResponse;
import kz.halyk.maqsat.travel.dto.TravelOfferDto;
import kz.halyk.maqsat.travel.repository.BookingRepository;
import kz.halyk.maqsat.travel.repository.TravelOfferNativeRepository;
import kz.halyk.maqsat.travel.repository.TravelOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final TravelOfferRepository offerRepository;
    private final TravelOfferNativeRepository nativeRepository;
    private final BookingRepository bookingRepository;

    public List<TravelOfferDto> listAll() {
        return offerRepository.findAll().stream()
                .map(TravelOfferDto::from)
                .toList();
    }

    public List<TravelOfferDto> listByAudience(List<String> audience) {
        if (audience == null || audience.isEmpty()) {
            return listAll();
        }
        String[] tags = audience.toArray(new String[0]);
        return nativeRepository.findActiveMatchingAudience(tags).stream()
                .map(TravelOfferDto::from)
                .toList();
    }

    public TravelOfferDto getById(UUID id) {
        TravelOffer offer = offerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Offer not found: " + id));
        return TravelOfferDto.from(offer);
    }

    @Transactional
    public ApplyDiscountResponse apply(ApplyDiscountRequest req) {
        TravelOffer offer = offerRepository.findById(req.offerId())
                .orElseThrow(() -> new NoSuchElementException("Offer not found: " + req.offerId()));

        BigDecimal finalPrice = offer.getBasePrice()
                .multiply(BigDecimal.ONE.subtract(offer.getDiscountPct().divide(new BigDecimal("100"))))
                .setScale(2, RoundingMode.HALF_UP);

        Booking booking = new Booking();
        booking.setUserId(req.userId());
        booking.setOfferId(offer.getId());
        booking.setFinalPrice(finalPrice);
        bookingRepository.save(booking);

        return new ApplyDiscountResponse(
                booking.getId(),
                offer.getId(),
                req.userId(),
                offer.getBasePrice(),
                offer.getDiscountPct(),
                finalPrice,
                booking.getBookedAt()
        );
    }
}
