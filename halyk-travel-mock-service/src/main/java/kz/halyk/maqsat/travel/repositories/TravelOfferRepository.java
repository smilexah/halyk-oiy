package kz.halyk.maqsat.travel.repositories;

import java.util.UUID;
import kz.halyk.maqsat.travel.entities.TravelOffer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelOfferRepository extends JpaRepository<TravelOffer, UUID> {}
