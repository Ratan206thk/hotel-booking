package com.rupeek.hotel.repository;

import com.rupeek.hotel.domain.Payment;
import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);

    Optional<Payment> findById(String id);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
