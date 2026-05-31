package kz.halyk.maqsat.alser.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.alser.domain.AppliedDiscount;
import kz.halyk.maqsat.alser.domain.DeviceOffer;
import kz.halyk.maqsat.alser.dto.ApplyDiscountRequest;
import kz.halyk.maqsat.alser.dto.ApplyDiscountResponse;
import kz.halyk.maqsat.alser.dto.DeviceOfferDto;
import kz.halyk.maqsat.alser.repository.AppliedDiscountRepository;
import kz.halyk.maqsat.alser.repository.DeviceOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final DeviceOfferRepository offers;
    private final AppliedDiscountRepository applied;

    public List<DeviceOfferDto> listAll() {
        return offers.findAll().stream().map(DeviceOfferDto::from).toList();
    }

    public List<DeviceOfferDto> matchingAudience(List<String> tags) {
        return offers.findActiveMatchingAudience(tags.toArray(new String[0]))
                .stream().map(DeviceOfferDto::from).toList();
    }

    public DeviceOfferDto get(UUID id) {
        return offers.findById(id).map(DeviceOfferDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "offer not found"));
    }

    @Transactional
    public ApplyDiscountResponse apply(ApplyDiscountRequest req) {
        DeviceOffer offer = offers.findById(req.offerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "offer not found"));
        BigDecimal pct = offer.getDiscountPct().divide(new BigDecimal("100"));
        BigDecimal finalPrice = offer.getBasePrice().multiply(BigDecimal.ONE.subtract(pct))
                .setScale(2, RoundingMode.HALF_UP);
        AppliedDiscount ad = new AppliedDiscount();
        ad.setUserId(req.userId());
        ad.setOfferId(offer.getId());
        ad.setFinalPrice(finalPrice);
        applied.save(ad);
        return new ApplyDiscountResponse(ad.getId(), offer.getId(), req.userId(),
                offer.getBasePrice(), offer.getDiscountPct(), finalPrice, ad.getAppliedAt());
    }
}
