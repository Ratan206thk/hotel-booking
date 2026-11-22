package com.rupeek.hotel.domain;

import com.rupeek.hotel.exception.ValidationException;
import java.math.BigDecimal;

/** A category of room in a property; {@code totalRooms} is the physical inventory of that category. */
public record RoomType(String id, String name, int capacity, BigDecimal pricePerNight, int totalRooms) {
    public RoomType {
        if (name == null || name.isBlank()) throw new ValidationException("Room type name is required");
        if (capacity < 1) throw new ValidationException("Room capacity must be at least 1");
        if (totalRooms < 1) throw new ValidationException("Total rooms must be at least 1");
        if (pricePerNight == null || pricePerNight.signum() <= 0) {
            throw new ValidationException("Price per night must be positive");
        }
    }
}
