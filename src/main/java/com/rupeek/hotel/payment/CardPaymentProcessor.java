package com.rupeek.hotel.payment;

import com.rupeek.hotel.domain.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class CardPaymentProcessor extends MockPaymentProcessor {
    static final String DECLINED_TEST_CARD = "4000000000000002";

    public CardPaymentProcessor() {
        super(PaymentMethod.CARD);
    }

    /** 16 digits, e.g. 4111111111111111. */
    @Override
    protected boolean isWellFormed(String instrument) {
        return instrument.matches("\\d{16}");
    }

    @Override
    protected boolean isDeclined(String instrument) {
        return DECLINED_TEST_CARD.equals(instrument);
    }
}
