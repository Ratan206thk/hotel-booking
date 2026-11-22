package com.rupeek.hotel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rupeek.hotel.domain.Amenity;
import com.rupeek.hotel.domain.Property;
import com.rupeek.hotel.exception.ValidationException;
import com.rupeek.hotel.search.PropertySearchResult;
import com.rupeek.hotel.search.SearchCriteria;
import com.rupeek.hotel.service.BookingCommand;
import com.rupeek.hotel.service.NewPropertyCommand;
import com.rupeek.hotel.service.NewRoomTypeCommand;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchServiceTest {

    private TestContext ctx;
    private Property taj;     // Mumbai/Colaba, 5*, 8000, wifi+pool
    private Property budget;  // Mumbai/Andheri, 2*, 1500, wifi
    private Property pune;    // different city

    @BeforeEach
    void setUp() {
        ctx = new TestContext();
        taj = ctx.addHotel("Taj", "Mumbai", "Colaba", 5, Set.of(Amenity.WIFI, Amenity.POOL), "8000", 3, 2);
        budget = ctx.addHotel("Budget Inn", "Mumbai", "Andheri", 2, Set.of(Amenity.WIFI), "1500", 2, 1);
        pune = ctx.addHotel("Pune Palace", "Pune", "Camp", 4, Set.of(Amenity.WIFI), "3000", 2, 1);
    }

    private List<String> names(int guests, Map<String, String> filters) {
        return ctx.searchService.search(new SearchCriteria("Mumbai",
                TestContext.stay("2026-12-10", "2026-12-12"), guests, filters)).stream()
                .map(r -> r.property().name()).sorted().toList();
    }

    @Test
    void returnsOnlyPropertiesInTheRequestedCityCaseInsensitively() {
        List<PropertySearchResult> results = ctx.searchService.search(new SearchCriteria("pune",
                TestContext.stay("2026-12-10", "2026-12-12"), 1, Map.of()));
        assertEquals(List.of("Pune Palace"), results.stream().map(r -> r.property().name()).toList());
    }

    @Test
    void offersCarryTotalPriceForTheStay() {
        PropertySearchResult result = ctx.searchService.search(new SearchCriteria("Mumbai",
                TestContext.stay("2026-12-10", "2026-12-12"), 1, Map.of("locality", "Colaba"))).get(0);
        assertEquals(new BigDecimal("16000.00"), result.offers().get(0).totalPrice());
    }

    @Test
    void excludesRoomsThatCannotFitTheParty() {
        assertEquals(List.of("Taj"), names(3, Map.of())); // only Taj's room sleeps 3
    }

    @Test
    void excludesPropertiesSoldOutForTheDatesButKeepsThemForOtherDates() {
        ctx.bookingService.book(new BookingCommand("g", budget.id(), budget.roomTypes().get(0).id(),
                TestContext.stay("2026-12-11", "2026-12-13"), 2));

        assertEquals(List.of("Taj"), names(1, Map.of()));
        List<PropertySearchResult> otherDates = ctx.searchService.search(new SearchCriteria("Mumbai",
                TestContext.stay("2026-12-20", "2026-12-22"), 1, Map.of()));
        assertEquals(2, otherDates.size());
    }

    @Test
    void filtersByPriceRange() {
        assertEquals(List.of("Budget Inn"), names(1, Map.of("maxPrice", "2000")));
        assertEquals(List.of("Taj"), names(1, Map.of("minPrice", "5000")));
        assertEquals(List.of("Budget Inn", "Taj"), names(1, Map.of("minPrice", "1000", "maxPrice", "9000")));
    }

    @Test
    void filtersByAmenitiesRequiringAllOfThem() {
        assertEquals(List.of("Budget Inn", "Taj"), names(1, Map.of("amenities", "wifi")));
        assertEquals(List.of("Taj"), names(1, Map.of("amenities", "WIFI, POOL")));
    }

    @Test
    void filtersByStarRatingAndLocality() {
        assertEquals(List.of("Taj"), names(1, Map.of("minStars", "4")));
        assertEquals(List.of("Budget Inn"), names(1, Map.of("locality", "andheri")));
    }

    @Test
    void combinesFilters() {
        assertTrue(names(1, Map.of("minStars", "4", "maxPrice", "2000")).isEmpty());
    }

    @Test
    void rejectsBadFilterValues() {
        assertThrows(ValidationException.class, () -> names(1, Map.of("minPrice", "cheap")));
        assertThrows(ValidationException.class, () -> names(1, Map.of("amenities", "JACUZZI")));
        assertThrows(ValidationException.class, () -> names(1, Map.of("minPrice", "9", "maxPrice", "1")));
    }

    @Test
    void multiPropertyOwnerAndStandalonePropertyShareTheSameModel() {
        var owner = ctx.ownerService.register("Oberoi Group");
        NewPropertyCommand cmd = new NewPropertyCommand("Oberoi Delhi", "Delhi", "CP", 5, Set.of(),
                List.of(new NewRoomTypeCommand("Suite", 2, new BigDecimal("9000"), 4)));
        ctx.propertyService.addProperty(owner.id(), cmd);
        ctx.propertyService.addProperty(owner.id(), cmd);

        assertEquals(2, ctx.ownerService.get(owner.id()).propertyIds().size());
        // the standalone hotel created in setUp is an owner with exactly one property
        assertEquals(List.of(taj.id()), ctx.ownerService.get(taj.ownerId()).propertyIds());
    }
}
