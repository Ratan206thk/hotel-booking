package com.rupeek.hotel;

import com.rupeek.hotel.cancellation.TieredCancellationPolicy;
import com.rupeek.hotel.domain.Amenity;
import com.rupeek.hotel.domain.DateRange;
import com.rupeek.hotel.domain.Property;
import com.rupeek.hotel.payment.CardPaymentProcessor;
import com.rupeek.hotel.payment.PaymentProcessorRegistry;
import com.rupeek.hotel.payment.PaymentService;
import com.rupeek.hotel.payment.UpiPaymentProcessor;
import com.rupeek.hotel.payment.WalletPaymentProcessor;
import com.rupeek.hotel.pricing.PerNightPricingStrategy;
import com.rupeek.hotel.repository.memory.InMemoryBookingRepository;
import com.rupeek.hotel.repository.memory.InMemoryInventoryRepository;
import com.rupeek.hotel.repository.memory.InMemoryOwnerRepository;
import com.rupeek.hotel.repository.memory.InMemoryPaymentRepository;
import com.rupeek.hotel.repository.memory.InMemoryPropertyRepository;
import com.rupeek.hotel.search.AmenityFilterFactory;
import com.rupeek.hotel.search.LocalityFilterFactory;
import com.rupeek.hotel.search.PriceRangeFilterFactory;
import com.rupeek.hotel.search.SearchService;
import com.rupeek.hotel.search.StarRatingFilterFactory;
import com.rupeek.hotel.service.BookingService;
import com.rupeek.hotel.service.NewPropertyCommand;
import com.rupeek.hotel.service.NewRoomTypeCommand;
import com.rupeek.hotel.service.OwnerService;
import com.rupeek.hotel.service.PropertyService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

/** Wires the real services over in-memory repositories (no Spring context) with a pinned clock. */
public class TestContext {

    /** "Now" is 2026-12-01 10:00 UTC. */
    public static final Clock CLOCK = Clock.fixed(Instant.parse("2026-12-01T10:00:00Z"), ZoneOffset.UTC);

    public final InMemoryInventoryRepository inventory = new InMemoryInventoryRepository();
    public final OwnerService ownerService = new OwnerService(new InMemoryOwnerRepository());
    public final PropertyService propertyService;
    public final BookingService bookingService;
    public final SearchService searchService;

    public TestContext() {
        InMemoryPropertyRepository properties = new InMemoryPropertyRepository();
        PaymentService paymentService = new PaymentService(
                new PaymentProcessorRegistry(List.of(
                        new CardPaymentProcessor(), new UpiPaymentProcessor(), new WalletPaymentProcessor())),
                new InMemoryPaymentRepository());
        PerNightPricingStrategy pricing = new PerNightPricingStrategy();

        propertyService = new PropertyService(ownerService, properties);
        bookingService = new BookingService(properties, new InMemoryBookingRepository(), inventory, pricing,
                paymentService, new TieredCancellationPolicy(CLOCK), CLOCK);
        searchService = new SearchService(properties, inventory, pricing, List.of(
                new LocalityFilterFactory(), new PriceRangeFilterFactory(),
                new AmenityFilterFactory(), new StarRatingFilterFactory()));
    }

    public Property addHotel(String name, String city, String locality, int stars, Set<Amenity> amenities,
                             String pricePerNight, int capacity, int totalRooms) {
        return propertyService.onboardStandalone(new NewPropertyCommand(name, city, locality, stars, amenities,
                List.of(new NewRoomTypeCommand("Deluxe", capacity, new BigDecimal(pricePerNight), totalRooms))));
    }

    /** A 5-star Mumbai hotel with a single 2-guest "Deluxe" room type. */
    public Property addHotel(int totalRooms) {
        return addHotel("Taj", "Mumbai", "Colaba", 5, Set.of(Amenity.WIFI, Amenity.POOL), "1000", 2, totalRooms);
    }

    public static DateRange stay(String checkIn, String checkOut) {
        return new DateRange(LocalDate.parse(checkIn), LocalDate.parse(checkOut));
    }
}
