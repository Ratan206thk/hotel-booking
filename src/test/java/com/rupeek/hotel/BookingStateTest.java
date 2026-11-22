package com.rupeek.hotel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.rupeek.hotel.domain.Booking;
import com.rupeek.hotel.domain.BookingStatus;
import com.rupeek.hotel.exception.InvalidStateException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BookingStateTest {

    private Booking newBooking() {
        return new Booking("b1", "g1", "p1", "r1", TestContext.stay("2026-12-10", "2026-12-12"), 2,
                new BigDecimal("2000.00"));
    }

    @Test
    void startsPendingAndConfirmsOnPayment() {
        Booking booking = newBooking();
        assertEquals(BookingStatus.PENDING_PAYMENT, booking.status());
        booking.confirm("pay-1");
        assertEquals(BookingStatus.CONFIRMED, booking.status());
        assertEquals("pay-1", booking.paymentId());
    }

    @Test
    void confirmedBookingCanBeCancelledWithRefundRecorded() {
        Booking booking = newBooking();
        booking.confirm("pay-1");
        booking.cancel(new BigDecimal("1000.00"));
        assertEquals(BookingStatus.CANCELLED, booking.status());
        assertEquals(new BigDecimal("1000.00"), booking.refundAmount());
    }

    @Test
    void terminalStatesRejectFurtherTransitions() {
        Booking cancelled = newBooking();
        cancelled.cancel(BigDecimal.ZERO);
        assertThrows(InvalidStateException.class, () -> cancelled.confirm("p"));
        assertThrows(InvalidStateException.class, () -> cancelled.cancel(BigDecimal.ZERO));

        Booking failed = newBooking();
        failed.markPaymentFailed();
        assertThrows(InvalidStateException.class, () -> failed.confirm("p"));
        assertThrows(InvalidStateException.class, failed::ensureCancellable);
    }

    @Test
    void cannotMarkConfirmedBookingAsPaymentFailed() {
        Booking booking = newBooking();
        booking.confirm("pay-1");
        assertThrows(InvalidStateException.class, booking::markPaymentFailed);
    }
}
