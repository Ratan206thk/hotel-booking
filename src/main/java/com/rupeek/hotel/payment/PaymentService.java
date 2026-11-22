package com.rupeek.hotel.payment;

import com.rupeek.hotel.domain.Payment;
import com.rupeek.hotel.domain.PaymentMethod;
import com.rupeek.hotel.exception.InvalidStateException;
import com.rupeek.hotel.exception.NotFoundException;
import com.rupeek.hotel.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Talks to payment processors and records the outcome. Knows nothing about bookings' lifecycle. */
@Service
public class PaymentService {

    private final PaymentProcessorRegistry registry;
    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentProcessorRegistry registry, PaymentRepository paymentRepository) {
        this.registry = registry;
        this.paymentRepository = paymentRepository;
    }

    public Payment charge(String bookingId, BigDecimal amount, PaymentMethod method,
                          String instrument, String idempotencyKey) {
        GatewayResult result = registry.forMethod(method).charge(amount, instrument);
        String paymentId = UUID.randomUUID().toString();
        Payment payment = result.success()
                ? Payment.succeeded(paymentId, bookingId, method, amount, idempotencyKey, result.reference())
                : Payment.failed(paymentId, bookingId, method, amount, idempotencyKey, result.message());
        return paymentRepository.save(payment);
    }

    public void refund(String paymentId, BigDecimal amount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + paymentId));
        GatewayResult result = registry.forMethod(payment.method()).refund(payment.gatewayReference(), amount);
        if (!result.success()) {
            throw new InvalidStateException("Refund failed: " + result.message());
        }
        payment.markRefunded(amount);
        paymentRepository.save(payment);
    }

    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey);
    }
}
