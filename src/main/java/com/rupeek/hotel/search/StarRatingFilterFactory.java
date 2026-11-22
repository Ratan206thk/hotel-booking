package com.rupeek.hotel.search;

import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class StarRatingFilterFactory implements PropertyFilterFactory {
    @Override
    public Optional<PropertyFilter> create(Map<String, String> params) {
        return FilterParams.integer(params, "minStars").<PropertyFilter>map(minStars ->
                c -> c.property().starRating() >= minStars);
    }
}
