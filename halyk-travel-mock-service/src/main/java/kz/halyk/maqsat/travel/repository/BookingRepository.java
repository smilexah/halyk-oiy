package kz.halyk.maqsat.travel.repository;

import kz.halyk.maqsat.travel.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
}
