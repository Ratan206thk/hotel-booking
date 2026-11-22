package com.rupeek.hotel.payment;

import com.rupeek.hotel.domain.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class UpiPaymentProcessor extends MockPaymentProcessor {
    public UpiPaymentProcessor() {
        super(PaymentMethod.UPI);
    }

    /** A VPA such as name@bank. */
    @Override
    protected boolean isWellFormed(String instrument) {
        return instrument.matches("[\\w.\\-]+@[\\w.\\-]+");
    }

    /** Any VPA whose handle contains "fail" is declined. */
    @Override
    protected boolean isDeclined(String instrument) {
        return instrument.toLowerCase().contains("fail");
    }
}
