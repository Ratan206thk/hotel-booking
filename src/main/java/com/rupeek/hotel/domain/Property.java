package com.rupeek.hotel.domain;

import com.rupeek.hotel.exception.ValidationException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record Property(String id, String ownerId, String name, Location location, int starRating,
                       Set<Amenity> amenities, List<RoomType> roomTypes) {

    public Property {
        if (name == null || name.isBlank()) throw new ValidationException("Property name is required");
        if (location == null) throw new ValidationException("Property location is required");
        if (starRating < 1 || starRating > 5) throw new ValidationException("Star rating must be between 1 and 5");
        if (roomTypes == null || roomTypes.isEmpty()) {
            throw new ValidationException("A property needs at least one room type");
        }
        amenities = amenities == null ? Set.of() : Set.copyOf(amenities);
        roomTypes = List.copyOf(roomTypes);
    }

    public Optional<RoomType> findRoomType(String roomTypeId) {
        return roomTypes.stream().filter(r -> r.id().equals(roomTypeId)).findFirst();
    }
}
