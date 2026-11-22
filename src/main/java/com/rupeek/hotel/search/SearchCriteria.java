package com.rupeek.hotel.search;

import com.rupeek.hotel.domain.DateRange;
import com.rupeek.hotel.exception.ValidationException;
import java.util.Map;

/** Core criteria (city, dates, guests) plus open-ended filter parameters interpreted by PropertyFilterFactories. */
public record SearchCriteria(String city, DateRange range, int guests, Map<String, String> filters) {
    public SearchCriteria {
        if (city == null || city.isBlank()) throw new ValidationException("City is required");
        if (range == null) throw new ValidationException("Check-in and check-out dates are required");
        if (guests < 1) throw new ValidationException("Guests must be at least 1");
        filters = filters == null ? Map.of() : Map.copyOf(filters);
    }
}
