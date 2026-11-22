package com.rupeek.hotel.payment;

import com.rupeek.hotel.domain.PaymentMethod;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stand-in for a third-party gateway. Subclasses define what a well-formed instrument looks like and
 * which instruments the "issuer" declines, so success and failure paths are easy to exercise.
 */
abstract class MockPaymentProcessor implements PaymentProcessor {

    private final PaymentMethod method;

    protected MockPaymentProcessor(PaymentMethod method) {
        this.method = method;
    }

    protected abstract boolean isWellFormed(String instrument);

    protected abstract boolean isDeclined(String instrument);

    @Override
    public PaymentMethod method() {
        return method;
    }

    @Override
    public GatewayResult charge(BigDecimal amount, String instrument) {
        if (instrument == null || !isWellFormed(instrument)) {
            return GatewayResult.declined("Invalid " + method + " details");
        }
        if (isDeclined(instrument)) {
            return GatewayResult.declined(method + " payment declined by issuer");
        }
        return GatewayResult.ok(method + "-" + UUID.randomUUID());
    }

    @Override
    public GatewayResult refund(String gatewayReference, BigDecimal amount) {
        return GatewayResult.ok("REFUND-" + gatewayReference);
    }
}
