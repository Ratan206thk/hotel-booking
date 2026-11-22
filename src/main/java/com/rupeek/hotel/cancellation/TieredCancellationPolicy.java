package com.rupeek.hotel.cancellation;

import com.rupeek.hotel.domain.Booking;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * 48h or more before check-in: 100% refund; 24h to 48h: 50%; under 24h: nothing.
 * Check-in is taken as 00:00 of the check-in date.
 */
@Component
public class TieredCancellationPolicy implements CancellationPolicy {

    private static final long FULL_REFUND_HOURS = 48;
    private static final long HALF_REFUND_HOURS = 24;

    private final Clock clock;

    public TieredCancellationPolicy(Clock clock) {
        this.clock = clock;
    }

    @Override
    public BigDecimal refundAmount(Booking booking) {
        long hoursToCheckIn = Duration.between(
                LocalDateTime.now(clock), booking.range().checkIn().atStartOfDay()).toHours();
        BigDecimal percent;
        if (hoursToCheckIn >= FULL_REFUND_HOURS) {
            percent = BigDecimal.ONE;
        } else if (hoursToCheckIn >= HALF_REFUND_HOURS) {
            percent = new BigDecimal("0.5");
        } else {
            percent = BigDecimal.ZERO;
        }
        return booking.totalAmount().multiply(percent).setScale(2, RoundingMode.HALF_UP);
    }
}
