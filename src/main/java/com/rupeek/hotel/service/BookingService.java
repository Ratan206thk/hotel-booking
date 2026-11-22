package com.rupeek.hotel.service;

import com.rupeek.hotel.cancellation.CancellationPolicy;
import com.rupeek.hotel.domain.Booking;
import com.rupeek.hotel.domain.BookingStatus;
import com.rupeek.hotel.domain.Payment;
import com.rupeek.hotel.domain.PaymentStatus;
import com.rupeek.hotel.domain.Property;
import com.rupeek.hotel.domain.RoomType;
import com.rupeek.hotel.exception.InvalidStateException;
import com.rupeek.hotel.exception.InventoryUnavailableException;
import com.rupeek.hotel.exception.NotFoundException;
import com.rupeek.hotel.exception.ValidationException;
import com.rupeek.hotel.payment.PaymentService;
import com.rupeek.hotel.pricing.PricingStrategy;
import com.rupeek.hotel.repository.BookingRepository;
import com.rupeek.hotel.repository.InventoryKey;
import com.rupeek.hotel.repository.InventoryRepository;
import com.rupeek.hotel.repository.PropertyRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Orchestrates the booking lifecycle: reserve inventory, take payment, cancel and refund. */
@Service
public class BookingService {

    private final PropertyRepository propertyRepository;
    private final BookingRepository bookingRepository;
    private final InventoryRepository inventoryRepository;
    private final PricingStrategy pricingStrategy;
    private final PaymentService paymentService;
    private final CancellationPolicy cancellationPolicy;
    private final Clock clock;

    public BookingService(PropertyRepository propertyRepository, BookingRepository bookingRepository,
                          InventoryRepository inventoryRepository, PricingStrategy pricingStrategy,
                          PaymentService paymentService, CancellationPolicy cancellationPolicy, Clock clock) {
        this.propertyRepository = propertyRepository;
        this.bookingRepository = bookingRepository;
        this.inventoryRepository = inventoryRepository;
        this.pricingStrategy = pricingStrategy;
        this.paymentService = paymentService;
        this.cancellationPolicy = cancellationPolicy;
        this.clock = clock;
    }

    /** Holds one room and creates a PENDING_PAYMENT booking. The hold is released if payment fails or on cancel. */
    public Booking book(BookingCommand command) {
        if (command.guestId() == null || command.guestId().isBlank()) {
            throw new ValidationException("Guest id is required");
        }
        if (command.range().checkIn().isBefore(LocalDate.now(clock))) {
            throw new ValidationException("Check-in date cannot be in the past");
        }
        Property property = propertyRepository.findById(command.propertyId())
                .orElseThrow(() -> new NotFoundException("Property not found: " + command.propertyId()));
        RoomType roomType = property.findRoomType(command.roomTypeId())
                .orElseThrow(() -> new NotFoundException("Room type not found: " + command.roomTypeId()));
        if (command.guests() < 1 || command.guests() > roomType.capacity()) {
            throw new ValidationException("Guests must be between 1 and " + roomType.capacity() + " for this room type");
        }

        InventoryKey key = new InventoryKey(property.id(), roomType.id());
        if (!inventoryRepository.tryReserve(key, command.range(), roomType.totalRooms())) {
            throw new InventoryUnavailableException("No rooms available for the requested dates");
        }
        try {
            Booking booking = new Booking(UUID.randomUUID().toString(), command.guestId(), property.id(),
                    roomType.id(), command.range(), command.guests(),
                    pricingStrategy.totalPrice(roomType, command.range()));
            return bookingRepository.save(booking);
        } catch (RuntimeException e) {
            inventoryRepository.release(key, command.range());
            throw e;
        }
    }

    /** The payment outcome drives the booking: success confirms it, failure releases the held room. */
    public PaymentOutcome pay(String bookingId, PaymentCommand command) {
        Booking booking = get(bookingId);
        synchronized (booking) {
            Optional<Payment> replay = paymentService.findByIdempotencyKey(command.idempotencyKey());
            if (replay.isPresent()) {
                if (!replay.get().bookingId().equals(bookingId)) {
                    throw new ValidationException("Idempotency key was already used for another booking");
                }
                return new PaymentOutcome(replay.get(), booking);
            }
            if (booking.status() != BookingStatus.PENDING_PAYMENT) {
                throw new InvalidStateException("Booking " + bookingId + " is not awaiting payment (state "
                        + booking.status() + ")");
            }

            Payment payment = paymentService.charge(bookingId, booking.totalAmount(), command.method(),
                    command.instrument(), command.idempotencyKey());
            if (payment.status() == PaymentStatus.SUCCESS) {
                booking.confirm(payment.id());
            } else {
                booking.markPaymentFailed();
                releaseInventory(booking);
            }
            bookingRepository.save(booking);
            return new PaymentOutcome(payment, booking);
        }
    }

    /** Applies the cancellation policy, refunds what it allows, and returns the room to inventory. */
    public Booking cancel(String bookingId) {
        Booking booking = get(bookingId);
        synchronized (booking) {
            booking.ensureCancellable();
            BigDecimal refund = BigDecimal.ZERO;
            if (booking.status() == BookingStatus.CONFIRMED) {
                refund = cancellationPolicy.refundAmount(booking);
                if (refund.signum() > 0) {
                    paymentService.refund(booking.paymentId(), refund);
                }
            }
            booking.cancel(refund);
            releaseInventory(booking);
            return bookingRepository.save(booking);
        }
    }

    public Booking get(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
    }

    private void releaseInventory(Booking booking) {
        inventoryRepository.release(new InventoryKey(booking.propertyId(), booking.roomTypeId()), booking.range());
    }
}
