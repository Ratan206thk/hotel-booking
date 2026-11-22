package com.rupeek.hotel.payment;

import com.rupeek.hotel.domain.PaymentMethod;
import com.rupeek.hotel.exception.ValidationException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Collects every PaymentProcessor bean: adding a payment method means adding one bean, nothing else changes. */
@Component
public class PaymentProcessorRegistry {

    private final Map<PaymentMethod, PaymentProcessor> processors = new EnumMap<>(PaymentMethod.class);

    public PaymentProcessorRegistry(List<PaymentProcessor> available) {
        available.forEach(p -> processors.put(p.method(), p));
    }

    public PaymentProcessor forMethod(PaymentMethod method) {
        PaymentProcessor processor = processors.get(method);
        if (processor == null) {
            throw new ValidationException("Unsupported payment method: " + method);
        }
        return processor;
    }
}
