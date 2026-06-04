package kz.halyk.maqsat.alser.services;

import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.alser.dto.req.ApplyDiscountRequest;
import kz.halyk.maqsat.alser.dto.res.ApplyDiscountResponse;
import kz.halyk.maqsat.alser.dto.res.DeviceOfferDto;

public interface OfferService {
    List<DeviceOfferDto> listAll();
    List<DeviceOfferDto> matchingAudience(List<String> tags);
    DeviceOfferDto get(UUID id);
    ApplyDiscountResponse apply(ApplyDiscountRequest req);
}
