package kz.halyk.maqsat.alser.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.alser.dto.req.ApplyDiscountRequest;
import kz.halyk.maqsat.alser.dto.res.ApplyDiscountResponse;
import kz.halyk.maqsat.alser.dto.res.DeviceOfferDto;
import kz.halyk.maqsat.alser.entities.AppliedDiscount;
import kz.halyk.maqsat.alser.entities.DeviceOffer;
import kz.halyk.maqsat.alser.mappers.DeviceOfferMapper;
import kz.halyk.maqsat.alser.repositories.AppliedDiscountRepository;
import kz.halyk.maqsat.alser.repositories.DeviceOfferRepository;
import kz.halyk.maqsat.alser.services.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OfferServiceImpl implements OfferService {

    private final DeviceOfferRepository offers;
    private final AppliedDiscountRepository applied;
    private final DeviceOfferMapper deviceOfferMapper;

    @Override
    public List<DeviceOfferDto> listAll() {
        return offers.findAll().stream().map(deviceOfferMapper::toDto).toList();
    }

    @Override
    public List<DeviceOfferDto> matchingAudience(List<String> tags) {
        return offers.findActiveMatchingAudience(tags.toArray(new String[0]))
                .stream().map(deviceOfferMapper::toDto).toList();
    }

    @Override
    public DeviceOfferDto get(UUID id) {
        return offers.findById(id).map(deviceOfferMapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "offer not found"));
    }

    @Override
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
