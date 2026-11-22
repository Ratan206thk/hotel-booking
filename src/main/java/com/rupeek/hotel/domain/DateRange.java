package com.rupeek.hotel.domain;

import com.rupeek.hotel.exception.ValidationException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Half-open stay interval [checkIn, checkOut): the guest occupies the nights checkIn .. checkOut-1. */
public record DateRange(LocalDate checkIn, LocalDate checkOut) {

    public DateRange {
        if (checkIn == null || checkOut == null) {
            throw new ValidationException("Check-in and check-out dates are required");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new ValidationException("Check-out must be after check-in");
        }
    }

    public long nights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** Every night of the stay. */
    public List<LocalDate> nightDates() {
        return checkIn.datesUntil(checkOut).toList();
    }
}
