package com.rupeek.hotel.cancellation;

import com.rupeek.hotel.domain.Booking;
import java.math.BigDecimal;

public interface CancellationPolicy {
    /** Amount to refund if the booking is cancelled right now; zero means no refund. */
    BigDecimal refundAmount(Booking booking);
}
