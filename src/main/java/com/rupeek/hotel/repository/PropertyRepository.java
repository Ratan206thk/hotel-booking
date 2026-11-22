package com.rupeek.hotel.repository;

import com.rupeek.hotel.domain.Property;
import java.util.List;
import java.util.Optional;

public interface PropertyRepository {
    Property save(Property property);

    Optional<Property> findById(String id);

    /** Case-insensitive city match. */
    List<Property> findByCity(String city);
}
