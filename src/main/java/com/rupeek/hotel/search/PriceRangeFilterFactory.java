package com.rupeek.hotel.search;

import com.rupeek.hotel.exception.ValidationException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Matches properties with at least one bookable room whose nightly price is within [minPrice, maxPrice]. */
@Component
public class PriceRangeFilterFactory implements PropertyFilterFactory {
    @Override
    public Optional<PropertyFilter> create(Map<String, String> params) {
        Optional<BigDecimal> min = FilterParams.decimal(params, "minPrice");
        Optional<BigDecimal> max = FilterParams.decimal(params, "maxPrice");
        if (min.isEmpty() && max.isEmpty()) {
            return Optional.empty();
        }
        if (min.isPresent() && max.isPresent() && min.get().compareTo(max.get()) > 0) {
            throw new ValidationException("minPrice cannot exceed maxPrice");
        }
        return Optional.of(c -> c.offers().stream().anyMatch(offer -> {
            BigDecimal price = offer.roomType().pricePerNight();
            return min.map(m -> price.compareTo(m) >= 0).orElse(true)
                    && max.map(m -> price.compareTo(m) <= 0).orElse(true);
        }));
    }
}
