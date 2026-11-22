package com.rupeek.hotel.pricing;

import com.rupeek.hotel.domain.DateRange;
import com.rupeek.hotel.domain.RoomType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class PerNightPricingStrategy implements PricingStrategy {

    @Override
    public BigDecimal totalPrice(RoomType roomType, DateRange range) {
        return roomType.pricePerNight()
                .multiply(BigDecimal.valueOf(range.nights()))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
