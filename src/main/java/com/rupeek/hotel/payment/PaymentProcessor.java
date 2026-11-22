package com.rupeek.hotel.payment;

import com.rupeek.hotel.domain.PaymentMethod;
import java.math.BigDecimal;

/** One implementation per payment method; the real gateway integration would live behind this. */
public interface PaymentProcessor {
    PaymentMethod method();

    GatewayResult charge(BigDecimal amount, String instrument);

    GatewayResult refund(String gatewayReference, BigDecimal amount);
}
