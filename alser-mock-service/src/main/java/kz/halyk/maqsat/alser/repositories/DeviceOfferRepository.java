package kz.halyk.maqsat.alser.repositories;

import java.util.UUID;
import kz.halyk.maqsat.alser.entities.DeviceOffer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceOfferRepository extends JpaRepository<DeviceOffer, UUID>, DeviceOfferRepositoryCustom {}
