package com.rupeek.hotel.search;

import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class LocalityFilterFactory implements PropertyFilterFactory {
    @Override
    public Optional<PropertyFilter> create(Map<String, String> params) {
        return FilterParams.text(params, "locality").<PropertyFilter>map(locality ->
                c -> c.property().location().locality().equalsIgnoreCase(locality));
    }
}
