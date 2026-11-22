package com.rupeek.hotel.repository;

import com.rupeek.hotel.domain.Booking;
import java.util.Optional;

public interface BookingRepository {
    Booking save(Booking booking);

    Optional<Booking> findById(String id);
}
