package kz.halyk.maqsat.travel.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import kz.halyk.maqsat.travel.dto.req.ApplyDiscountRequest;
import kz.halyk.maqsat.travel.dto.res.ApplyDiscountResponse;
import kz.halyk.maqsat.travel.dto.res.TravelOfferDto;
import kz.halyk.maqsat.travel.entities.Booking;
import kz.halyk.maqsat.travel.entities.TravelOffer;
import kz.halyk.maqsat.travel.mappers.TravelOfferMapper;
import kz.halyk.maqsat.travel.repositories.BookingRepository;
import kz.halyk.maqsat.travel.repositories.TravelOfferNativeRepository;
import kz.halyk.maqsat.travel.repositories.TravelOfferRepository;
import kz.halyk.maqsat.travel.services.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OfferServiceImpl implements OfferService {

    private final TravelOfferRepository offerRepository;
    private final TravelOfferNativeRepository nativeRepository;
    private final BookingRepository bookingRepository;
    private final TravelOfferMapper travelOfferMapper;

    @Override
    public List<TravelOfferDto> listAll() {
        return offerRepository.findAll().stream().map(travelOfferMapper::toDto).toList();
    }

    @Override
    public List<TravelOfferDto> listByAudience(List<String> audience) {
        if (audience == null || audience.isEmpty()) {
            return listAll();
        }
        return nativeRepository.findActiveMatchingAudience(audience.toArray(new String[0]))
                .stream().map(travelOfferMapper::toDto).toList();
    }

    @Override
    public TravelOfferDto getById(UUID id) {
        TravelOffer offer = offerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Offer not found: " + id));
        return travelOfferMapper.toDto(offer);
    }

    @Override
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
                booking.getId(), offer.getId(), req.userId(),
                offer.getBasePrice(), offer.getDiscountPct(), finalPrice, booking.getBookedAt());
    }
}
