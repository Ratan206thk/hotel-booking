package com.rupeek.hotel.search;

import java.util.Map;
import java.util.Optional;

/**
 * Builds a filter from the raw request parameters if (and only if) the parameters it understands are present.
 * To add a filter, add one @Component implementing this interface -- SearchService is never touched.
 */
public interface PropertyFilterFactory {
    Optional<PropertyFilter> create(Map<String, String> params);
}
