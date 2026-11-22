package com.rupeek.hotel.service;

import com.rupeek.hotel.domain.Booking;
import com.rupeek.hotel.domain.Payment;

public record PaymentOutcome(Payment payment, Booking booking) {
}
