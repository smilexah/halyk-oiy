package kz.halyk.maqsat.alser.repository;

import java.util.UUID;
import kz.halyk.maqsat.alser.domain.DeviceOffer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceOfferRepository extends JpaRepository<DeviceOffer, UUID>, DeviceOfferRepositoryCustom {
}
