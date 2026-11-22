package com.rupeek.hotel.service;

import com.rupeek.hotel.domain.PaymentMethod;

public record PaymentCommand(PaymentMethod method, String instrument, String idempotencyKey) {
}
