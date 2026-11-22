package com.rupeek.hotel.search;

import com.rupeek.hotel.exception.ValidationException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/** Typed, validated access to raw string filter parameters. */
final class FilterParams {
    private FilterParams() {
    }

    static Optional<String> text(Map<String, String> params, String key) {
        String value = params.get(key);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
    }

    static Optional<BigDecimal> decimal(Map<String, String> params, String key) {
        return text(params, key).map(v -> {
            try {
                return new BigDecimal(v);
            } catch (NumberFormatException e) {
                throw new ValidationException("Parameter '" + key + "' must be a number");
            }
        });
    }

    static Optional<Integer> integer(Map<String, String> params, String key) {
        return text(params, key).map(v -> {
            try {
                return Integer.valueOf(v);
            } catch (NumberFormatException e) {
                throw new ValidationException("Parameter '" + key + "' must be an integer");
            }
        });
    }
}
