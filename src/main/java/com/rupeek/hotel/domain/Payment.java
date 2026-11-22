package com.rupeek.hotel.domain;

import com.rupeek.hotel.exception.InvalidStateException;
import java.math.BigDecimal;

public class Payment {
    private final String id;
    private final String bookingId;
    private final PaymentMethod method;
    private final BigDecimal amount;
    private final String idempotencyKey;
    private final String gatewayReference;
    private final String failureReason;

    private PaymentStatus status;
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    private Payment(String id, String bookingId, PaymentMethod method, BigDecimal amount, String idempotencyKey,
                    PaymentStatus status, String gatewayReference, String failureReason) {
        this.id = id;
        this.bookingId = bookingId;
        this.method = method;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.gatewayReference = gatewayReference;
        this.failureReason = failureReason;
    }

    public static Payment succeeded(String id, String bookingId, PaymentMethod method, BigDecimal amount,
                                    String idempotencyKey, String gatewayReference) {
        return new Payment(id, bookingId, method, amount, idempotencyKey, PaymentStatus.SUCCESS, gatewayReference, null);
    }

    public static Payment failed(String id, String bookingId, PaymentMethod method, BigDecimal amount,
                                 String idempotencyKey, String reason) {
        return new Payment(id, bookingId, method, amount, idempotencyKey, PaymentStatus.FAILED, null, reason);
    }

    public synchronized void markRefunded(BigDecimal refund) {
        if (status != PaymentStatus.SUCCESS) {
            throw new InvalidStateException("Only a successful payment can be refunded (status " + status + ")");
        }
        if (refund.compareTo(amount) > 0) {
            throw new InvalidStateException("Refund exceeds the amount paid");
        }
        this.refundedAmount = refund;
        this.status = PaymentStatus.REFUNDED;
    }

    public String id() { return id; }
    public String bookingId() { return bookingId; }
    public PaymentMethod method() { return method; }
    public BigDecimal amount() { return amount; }
    public String idempotencyKey() { return idempotencyKey; }
    public String gatewayReference() { return gatewayReference; }
    public String failureReason() { return failureReason; }
    public synchronized PaymentStatus status() { return status; }
    public synchronized BigDecimal refundedAmount() { return refundedAmount; }
}
