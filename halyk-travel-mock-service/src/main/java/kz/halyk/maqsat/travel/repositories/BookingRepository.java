package kz.halyk.maqsat.travel.repositories;

import java.util.UUID;
import kz.halyk.maqsat.travel.entities.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, UUID> {}
