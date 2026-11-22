package com.rupeek.hotel.repository.memory;

import com.rupeek.hotel.domain.Booking;
import com.rupeek.hotel.repository.BookingRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryBookingRepository implements BookingRepository {
    private final Map<String, Booking> store = new ConcurrentHashMap<>();

    @Override
    public Booking save(Booking booking) {
        store.put(booking.id(), booking);
        return booking;
    }

    @Override
    public Optional<Booking> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }
}
