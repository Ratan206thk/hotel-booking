package com.rupeek.hotel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rupeek.hotel.domain.Booking;
import com.rupeek.hotel.domain.BookingStatus;
import com.rupeek.hotel.domain.DateRange;
import com.rupeek.hotel.domain.PaymentMethod;
import com.rupeek.hotel.domain.PaymentStatus;
import com.rupeek.hotel.domain.Property;
import com.rupeek.hotel.exception.InvalidStateException;
import com.rupeek.hotel.exception.InventoryUnavailableException;
import com.rupeek.hotel.exception.NotFoundException;
import com.rupeek.hotel.exception.ValidationException;
import com.rupeek.hotel.service.BookingCommand;
import com.rupeek.hotel.service.PaymentCommand;
import com.rupeek.hotel.service.PaymentOutcome;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BookingServiceTest {

    private static final String GOOD_CARD = "4111111111111111";

    private TestContext ctx;
    private Property hotel;

    @BeforeEach
    void setUp() {
        ctx = new TestContext();
        hotel = ctx.addHotel(1); // a single room makes double-booking easy to provoke
    }

    private Booking book(String checkIn, String checkOut, int guests) {
        return book(TestContext.stay(checkIn, checkOut), guests);
    }

    private Booking book(DateRange range, int guests) {
        return ctx.bookingService.book(new BookingCommand("guest-1", hotel.id(),
                hotel.roomTypes().get(0).id(), range, guests));
    }

    private PaymentOutcome payByCard(Booking booking, String key) {
        return ctx.bookingService.pay(booking.id(), new PaymentCommand(PaymentMethod.CARD, GOOD_CARD, key));
    }

    private boolean isFree(String checkIn, String checkOut) {
        return ctx.searchService.search(new com.rupeek.hotel.search.SearchCriteria(
                "Mumbai", TestContext.stay(checkIn, checkOut), 1, java.util.Map.of())).size() == 1;
    }

    // ---- booking ----

    @Test
    void newBookingIsPendingPaymentAndPricedPerNight() {
        Booking booking = book("2026-12-10", "2026-12-13", 2);
        assertEquals(BookingStatus.PENDING_PAYMENT, booking.status());
        assertEquals(new BigDecimal("3000.00"), booking.totalAmount());
    }

    @Test
    void overlappingBookingOfSoldOutRoomIsRejected() {
        book("2026-12-10", "2026-12-13", 2);
        assertThrows(InventoryUnavailableException.class, () -> book("2026-12-12", "2026-12-14", 2));
    }

    @Test
    void nonOverlappingDatesCanStillBeBooked() {
        book("2026-12-10", "2026-12-13", 2);
        Booking later = book("2026-12-13", "2026-12-15", 2);
        assertEquals(BookingStatus.PENDING_PAYMENT, later.status());
    }

    @Test
    void rejectsBookingsThatBreakBusinessRules() {
        assertThrows(ValidationException.class, () -> book("2026-12-10", "2026-12-12", 3)); // capacity is 2
        assertThrows(ValidationException.class, () -> book("2026-12-10", "2026-12-12", 0));
        assertThrows(ValidationException.class, () -> book("2026-11-20", "2026-11-22", 1)); // in the past
        assertThrows(NotFoundException.class, () -> ctx.bookingService.book(new BookingCommand(
                "g", "nope", "nope", TestContext.stay("2026-12-10", "2026-12-12"), 1)));
        assertThrows(NotFoundException.class, () -> ctx.bookingService.book(new BookingCommand(
                "g", hotel.id(), "nope", TestContext.stay("2026-12-10", "2026-12-12"), 1)));
    }

    // ---- payment ----

    @Test
    void successfulPaymentConfirmsBooking() {
        Booking booking = book("2026-12-10", "2026-12-12", 2);
        PaymentOutcome outcome = payByCard(booking, "k1");
        assertEquals(PaymentStatus.SUCCESS, outcome.payment().status());
        assertEquals(BookingStatus.CONFIRMED, outcome.booking().status());
        assertEquals(outcome.payment().id(), outcome.booking().paymentId());
    }

    @Test
    void failedPaymentFailsBookingAndReleasesInventory() {
        Booking booking = book("2026-12-10", "2026-12-12", 2);
        PaymentOutcome outcome = ctx.bookingService.pay(booking.id(),
                new PaymentCommand(PaymentMethod.UPI, "fail@upi", "k1"));
        assertEquals(PaymentStatus.FAILED, outcome.payment().status());
        assertEquals(BookingStatus.PAYMENT_FAILED, outcome.booking().status());
        assertTrue(isFree("2026-12-10", "2026-12-12"));
    }

    @Test
    void worksWithEveryPaymentMethod() {
        Property big = ctx.addHotel("Big", "Pune", "Camp", 3, java.util.Set.of(), "500", 2, 3);
        String roomTypeId = big.roomTypes().get(0).id();
        DateRange stay = TestContext.stay("2026-12-10", "2026-12-11");
        String[][] cases = {{"CARD", GOOD_CARD}, {"UPI", "guest@okbank"}, {"WALLET", "wallet-42"}};
        for (String[] c : cases) {
            Booking b = ctx.bookingService.book(new BookingCommand("g", big.id(), roomTypeId, stay, 1));
            PaymentOutcome outcome = ctx.bookingService.pay(b.id(),
                    new PaymentCommand(PaymentMethod.valueOf(c[0]), c[1], "key-" + c[0]));
            assertEquals(BookingStatus.CONFIRMED, outcome.booking().status(), c[0]);
        }
    }

    @Test
    void paymentWithMalformedInstrumentIsDeclined() {
        Booking booking = book("2026-12-10", "2026-12-12", 2);
        PaymentOutcome outcome = ctx.bookingService.pay(booking.id(),
                new PaymentCommand(PaymentMethod.CARD, "1234", "k1"));
        assertEquals(PaymentStatus.FAILED, outcome.payment().status());
        assertEquals(BookingStatus.PAYMENT_FAILED, outcome.booking().status());
    }

    @Test
    void repeatingPaymentWithSameIdempotencyKeyReturnsOriginalResultWithoutRecharging() {
        Booking booking = book("2026-12-10", "2026-12-12", 2);
        PaymentOutcome first = payByCard(booking, "same-key");
        PaymentOutcome second = payByCard(booking, "same-key");
        assertSame(first.payment(), second.payment());
        assertEquals(BookingStatus.CONFIRMED, second.booking().status());
    }

    @Test
    void idempotencyKeyCannotBeReusedAcrossBookings() {
        Booking first = book("2026-12-10", "2026-12-12", 2);
        Booking second = book("2026-12-12", "2026-12-14", 2);
        payByCard(first, "shared-key");
        assertThrows(ValidationException.class, () -> payByCard(second, "shared-key"));
    }

    @Test
    void cannotPayAgainForConfirmedOrCancelledBooking() {
        Booking booking = book("2026-12-10", "2026-12-12", 2);
        payByCard(booking, "k1");
        assertThrows(InvalidStateException.class, () -> payByCard(booking, "k2"));

        ctx.bookingService.cancel(booking.id());
        assertThrows(InvalidStateException.class, () -> payByCard(booking, "k3"));
    }

    // ---- cancellation ----

    @Test
    void cancellingFarAheadRefundsInFullAndFreesInventory() {
        Booking booking = book("2026-12-10", "2026-12-12", 2); // 9 days away
        payByCard(booking, "k1");

        Booking cancelled = ctx.bookingService.cancel(booking.id());

        assertEquals(BookingStatus.CANCELLED, cancelled.status());
        assertEquals(new BigDecimal("2000.00"), cancelled.refundAmount());
        assertTrue(isFree("2026-12-10", "2026-12-12"));
        // and it can be booked again
        assertEquals(BookingStatus.PENDING_PAYMENT, book("2026-12-10", "2026-12-12", 2).status());
    }

    @Test
    void cancellingBetween24And48HoursBeforeCheckInRefundsHalf() {
        Booking booking = book("2026-12-03", "2026-12-04", 2); // check-in is 38h away
        payByCard(booking, "k1");
        assertEquals(new BigDecimal("500.00"), ctx.bookingService.cancel(booking.id()).refundAmount());
    }

    @Test
    void cancellingWithinADayOfCheckInRefundsNothingButStillReleasesRoom() {
        Booking booking = book("2026-12-02", "2026-12-03", 2); // check-in is 14h away
        payByCard(booking, "k1");
        Booking cancelled = ctx.bookingService.cancel(booking.id());
        assertEquals(0, cancelled.refundAmount().signum());
        assertTrue(isFree("2026-12-02", "2026-12-03"));
    }

    @Test
    void unpaidBookingCanBeCancelledWithNoRefund() {
        Booking booking = book("2026-12-10", "2026-12-12", 2);
        Booking cancelled = ctx.bookingService.cancel(booking.id());
        assertEquals(BookingStatus.CANCELLED, cancelled.status());
        assertEquals(0, cancelled.refundAmount().signum());
        assertTrue(isFree("2026-12-10", "2026-12-12"));
    }

    @Test
    void cancellingTwiceIsAnInvalidTransitionAndDoesNotReleaseInventoryTwice() {
        Booking booking = book("2026-12-10", "2026-12-12", 2);
        ctx.bookingService.cancel(booking.id());
        assertThrows(InvalidStateException.class, () -> ctx.bookingService.cancel(booking.id()));
        // if inventory had been released twice, a second room could now be oversold
        book("2026-12-10", "2026-12-12", 2);
        assertThrows(InventoryUnavailableException.class, () -> book("2026-12-10", "2026-12-12", 2));
    }

    @Test
    void cancellingUnknownBookingIsNotFound() {
        assertThrows(NotFoundException.class, () -> ctx.bookingService.cancel("missing"));
    }
}
