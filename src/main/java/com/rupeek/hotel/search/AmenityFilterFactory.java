package com.rupeek.hotel.search;

import com.rupeek.hotel.domain.Amenity;
import com.rupeek.hotel.exception.ValidationException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** {@code amenities=WIFI,POOL} -- the property must offer all of them. */
@Component
public class AmenityFilterFactory implements PropertyFilterFactory {
    @Override
    public Optional<PropertyFilter> create(Map<String, String> params) {
        return FilterParams.text(params, "amenities").<PropertyFilter>map(csv -> {
            Set<Amenity> required = Arrays.stream(csv.split(","))
                    .map(String::trim)
                    .map(this::parse)
                    .collect(Collectors.toSet());
            return c -> c.property().amenities().containsAll(required);
        });
    }

    private Amenity parse(String name) {
        try {
            return Amenity.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Unknown amenity: " + name);
        }
    }
}
