package com.rupeek.hotel.domain;

import com.rupeek.hotel.exception.InvalidStateException;
import java.math.BigDecimal;

/** One room of one room type for a date range. All state changes go through guarded transitions. */
public class Booking {
    private final String id;
    private final String guestId;
    private final String propertyId;
    private final String roomTypeId;
    private final DateRange range;
    private final int guests;
    private final BigDecimal totalAmount;

    private BookingStatus status = BookingStatus.PENDING_PAYMENT;
    private String paymentId;
    private BigDecimal refundAmount = BigDecimal.ZERO;

    public Booking(String id, String guestId, String propertyId, String roomTypeId,
                   DateRange range, int guests, BigDecimal totalAmount) {
        this.id = id;
        this.guestId = guestId;
        this.propertyId = propertyId;
        this.roomTypeId = roomTypeId;
        this.range = range;
        this.guests = guests;
        this.totalAmount = totalAmount;
    }

    public synchronized void confirm(String paymentId) {
        transitionTo(BookingStatus.CONFIRMED);
        this.paymentId = paymentId;
    }

    public synchronized void markPaymentFailed() {
        transitionTo(BookingStatus.PAYMENT_FAILED);
    }

    public synchronized void cancel(BigDecimal refundAmount) {
        transitionTo(BookingStatus.CANCELLED);
        this.refundAmount = refundAmount;
    }

    /** Fails fast, before any side effect (refund, inventory release) is attempted. */
    public synchronized void ensureCancellable() {
        if (!status.canTransitionTo(BookingStatus.CANCELLED)) {
            throw new InvalidStateException("Booking " + id + " cannot be cancelled from state " + status);
        }
    }

    private void transitionTo(BookingStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidStateException("Invalid booking transition " + status + " -> " + next);
        }
        status = next;
    }

    public String id() { return id; }
    public String guestId() { return guestId; }
    public String propertyId() { return propertyId; }
    public String roomTypeId() { return roomTypeId; }
    public DateRange range() { return range; }
    public int guests() { return guests; }
    public BigDecimal totalAmount() { return totalAmount; }
    public synchronized BookingStatus status() { return status; }
    public synchronized String paymentId() { return paymentId; }
    public synchronized BigDecimal refundAmount() { return refundAmount; }
}
