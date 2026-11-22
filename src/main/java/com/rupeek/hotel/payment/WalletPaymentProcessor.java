package com.rupeek.hotel.payment;

import com.rupeek.hotel.domain.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class WalletPaymentProcessor extends MockPaymentProcessor {
    public WalletPaymentProcessor() {
        super(PaymentMethod.WALLET);
    }

    /** Any non-blank wallet id. */
    @Override
    protected boolean isWellFormed(String instrument) {
        return !instrument.isBlank();
    }

    /** Any wallet id containing "fail" is treated as having insufficient balance. */
    @Override
    protected boolean isDeclined(String instrument) {
        return instrument.toLowerCase().contains("fail");
    }
}
