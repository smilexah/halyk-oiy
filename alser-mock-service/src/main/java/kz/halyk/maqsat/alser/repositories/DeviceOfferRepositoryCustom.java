package kz.halyk.maqsat.alser.repositories;

import java.util.List;
import kz.halyk.maqsat.alser.entities.DeviceOffer;

public interface DeviceOfferRepositoryCustom {
    List<DeviceOffer> findActiveMatchingAudience(String[] tags);
}
