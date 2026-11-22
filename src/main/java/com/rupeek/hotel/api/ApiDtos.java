package com.rupeek.hotel.api;

import com.rupeek.hotel.domain.Amenity;
import com.rupeek.hotel.domain.Booking;
import com.rupeek.hotel.domain.BookingStatus;
import com.rupeek.hotel.domain.Owner;
import com.rupeek.hotel.domain.Payment;
import com.rupeek.hotel.domain.PaymentMethod;
import com.rupeek.hotel.domain.PaymentStatus;
import com.rupeek.hotel.domain.Property;
import com.rupeek.hotel.domain.RoomType;
import com.rupeek.hotel.search.PropertySearchResult;
import com.rupeek.hotel.service.NewPropertyCommand;
import com.rupeek.hotel.service.NewRoomTypeCommand;
import com.rupeek.hotel.service.PaymentOutcome;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Request/response shapes of the REST API, kept apart from the domain model. */
public final class ApiDtos {
    private ApiDtos() {
    }

    // ----- requests -----

    public record RoomTypeRequest(
            @NotBlank String name,
            @Min(1) int capacity,
            @NotNull @DecimalMin("0.01") BigDecimal pricePerNight,
            @Min(1) int totalRooms) {
        NewRoomTypeCommand toCommand() {
            return new NewRoomTypeCommand(name, capacity, pricePerNight, totalRooms);
        }
    }

    public record PropertyRequest(
            @NotBlank String name,
            @NotBlank String city,
            String locality,
            @Min(1) @Max(5) int starRating,
            Set<Amenity> amenities,
            @NotEmpty @Valid List<RoomTypeRequest> roomTypes) {
        NewPropertyCommand toCommand() {
            return new NewPropertyCommand(name, city, locality, starRating, amenities,
                    roomTypes.stream().map(RoomTypeRequest::toCommand).toList());
        }
    }

    /** {@code properties} is optional: owners can be registered empty and add properties later. */
    public record OwnerRequest(@NotBlank String name, @Valid List<PropertyRequest> properties) {
    }

    public record BookingRequest(
            @NotBlank String guestId,
            @NotBlank String propertyId,
            @NotBlank String roomTypeId,
            @NotNull LocalDate checkIn,
            @NotNull LocalDate checkOut,
            @Min(1) int guests) {
    }

    public record PayRequest(
            @NotNull PaymentMethod method,
            @NotBlank String instrument,
            @NotBlank String idempotencyKey) {
    }

    // ----- responses -----

    public record ErrorResponse(String error, String message) {
    }

    public record RoomTypeResponse(String id, String name, int capacity, BigDecimal pricePerNight, int totalRooms) {
        static RoomTypeResponse from(RoomType r) {
            return new RoomTypeResponse(r.id(), r.name(), r.capacity(), r.pricePerNight(), r.totalRooms());
        }
    }

    public record PropertyResponse(String id, String ownerId, String name, String city, String locality,
                                   int starRating, Set<Amenity> amenities, List<RoomTypeResponse> roomTypes) {
        static PropertyResponse from(Property p) {
            return new PropertyResponse(p.id(), p.ownerId(), p.name(), p.location().city(), p.location().locality(),
                    p.starRating(), p.amenities(), p.roomTypes().stream().map(RoomTypeResponse::from).toList());
        }
    }

    public record OwnerResponse(String id, String name, List<String> propertyIds) {
        static OwnerResponse from(Owner o) {
            return new OwnerResponse(o.id(), o.name(), o.propertyIds());
        }
    }

    public record OfferResponse(String roomTypeId, String roomTypeName, int capacity, int availableRooms,
                                BigDecimal pricePerNight, BigDecimal totalPrice) {
    }

    public record SearchResultResponse(PropertyResponse property, List<OfferResponse> offers) {
        static SearchResultResponse from(PropertySearchResult r) {
            List<OfferResponse> offers = r.offers().stream()
                    .map(o -> new OfferResponse(o.roomType().id(), o.roomType().name(), o.roomType().capacity(),
                            o.availableRooms(), o.roomType().pricePerNight(), o.totalPrice()))
                    .toList();
            return new SearchResultResponse(PropertyResponse.from(r.property()), offers);
        }
    }

    public record BookingResponse(String id, String guestId, String propertyId, String roomTypeId,
                                  LocalDate checkIn, LocalDate checkOut, int guests, BigDecimal totalAmount,
                                  BookingStatus status, String paymentId, BigDecimal refundAmount) {
        static BookingResponse from(Booking b) {
            return new BookingResponse(b.id(), b.guestId(), b.propertyId(), b.roomTypeId(),
                    b.range().checkIn(), b.range().checkOut(), b.guests(), b.totalAmount(),
                    b.status(), b.paymentId(), b.refundAmount());
        }
    }

    public record PaymentResponse(String id, String bookingId, PaymentMethod method, BigDecimal amount,
                                  PaymentStatus status, String failureReason, BigDecimal refundedAmount) {
        static PaymentResponse from(Payment p) {
            return new PaymentResponse(p.id(), p.bookingId(), p.method(), p.amount(), p.status(),
                    p.failureReason(), p.refundedAmount());
        }
    }

    public record PaymentOutcomeResponse(PaymentResponse payment, BookingResponse booking) {
        static PaymentOutcomeResponse from(PaymentOutcome o) {
            return new PaymentOutcomeResponse(PaymentResponse.from(o.payment()), BookingResponse.from(o.booking()));
        }
    }
}
