package kz.halyk.maqsat.travel.repository;

import kz.halyk.maqsat.travel.domain.TravelOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TravelOfferRepository extends JpaRepository<TravelOffer, UUID> {
}
