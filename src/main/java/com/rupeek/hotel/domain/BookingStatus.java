package com.rupeek.hotel.domain;

/**
 * PENDING_PAYMENT --pay ok--> CONFIRMED --cancel--> CANCELLED
 *       |--pay failed--> PAYMENT_FAILED (terminal)
 *       |--cancel------> CANCELLED      (terminal)
 */
public enum BookingStatus {
    PENDING_PAYMENT, CONFIRMED, PAYMENT_FAILED, CANCELLED;

    public boolean canTransitionTo(BookingStatus next) {
        return switch (this) {
            case PENDING_PAYMENT -> next == CONFIRMED || next == PAYMENT_FAILED || next == CANCELLED;
            case CONFIRMED -> next == CANCELLED;
            case PAYMENT_FAILED, CANCELLED -> false;
        };
    }
}
