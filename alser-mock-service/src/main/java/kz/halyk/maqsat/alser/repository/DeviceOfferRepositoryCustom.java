package kz.halyk.maqsat.alser.repository;

import java.util.List;
import kz.halyk.maqsat.alser.domain.DeviceOffer;

public interface DeviceOfferRepositoryCustom {

    List<DeviceOffer> findActiveMatchingAudience(String[] tags);
}
