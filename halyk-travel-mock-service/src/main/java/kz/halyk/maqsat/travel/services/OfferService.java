package kz.halyk.maqsat.travel.services;

import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.travel.dto.req.ApplyDiscountRequest;
import kz.halyk.maqsat.travel.dto.res.ApplyDiscountResponse;
import kz.halyk.maqsat.travel.dto.res.TravelOfferDto;

public interface OfferService {
    List<TravelOfferDto> listAll();
    List<TravelOfferDto> listByAudience(List<String> audience);
    TravelOfferDto getById(UUID id);
    ApplyDiscountResponse apply(ApplyDiscountRequest req);
}
