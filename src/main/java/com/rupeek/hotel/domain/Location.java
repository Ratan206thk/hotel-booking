package com.rupeek.hotel.domain;

import com.rupeek.hotel.exception.ValidationException;

public record Location(String city, String locality) {
    public Location {
        if (city == null || city.isBlank()) {
            throw new ValidationException("City is required");
        }
        city = city.trim();
        locality = locality == null ? "" : locality.trim();
    }
}
