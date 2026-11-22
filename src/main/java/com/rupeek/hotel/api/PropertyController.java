package com.rupeek.hotel.api;

import com.rupeek.hotel.api.ApiDtos.PropertyRequest;
import com.rupeek.hotel.api.ApiDtos.PropertyResponse;
import com.rupeek.hotel.api.ApiDtos.SearchResultResponse;
import com.rupeek.hotel.domain.DateRange;
import com.rupeek.hotel.exception.ValidationException;
import com.rupeek.hotel.search.SearchCriteria;
import com.rupeek.hotel.search.SearchService;
import com.rupeek.hotel.service.PropertyService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;
    private final SearchService searchService;

    public PropertyController(PropertyService propertyService, SearchService searchService) {
        this.propertyService = propertyService;
        this.searchService = searchService;
    }

    /** Onboards a single standalone property (creates a one-property owner account behind the scenes). */
    @PostMapping("/standalone")
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse onboardStandalone(@Valid @RequestBody PropertyRequest request) {
        return PropertyResponse.from(propertyService.onboardStandalone(request.toCommand()));
    }

    @GetMapping("/{propertyId}")
    public PropertyResponse get(@PathVariable String propertyId) {
        return PropertyResponse.from(propertyService.get(propertyId));
    }

    /**
     * Required: city, checkIn, checkOut (yyyy-MM-dd), guests.
     * Optional filters (any others are ignored): locality, minPrice, maxPrice, amenities (csv), minStars.
     */
    @GetMapping("/search")
    public List<SearchResultResponse> search(@RequestParam String city,
                                             @RequestParam String checkIn,
                                             @RequestParam String checkOut,
                                             @RequestParam(defaultValue = "1") int guests,
                                             @RequestParam Map<String, String> allParams) {
        DateRange range = new DateRange(parseDate("checkIn", checkIn), parseDate("checkOut", checkOut));
        return searchService.search(new SearchCriteria(city, range, guests, allParams)).stream()
                .map(SearchResultResponse::from)
                .toList();
    }

    private static LocalDate parseDate(String name, String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new ValidationException("'" + name + "' must be a date in yyyy-MM-dd format");
        }
    }
}
