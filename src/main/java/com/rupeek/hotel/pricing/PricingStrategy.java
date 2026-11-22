package com.rupeek.hotel.pricing;

import com.rupeek.hotel.domain.DateRange;
import com.rupeek.hotel.domain.RoomType;
import java.math.BigDecimal;

/** Seam for dynamic pricing (weekend surcharge, seasonal rates, demand-based...). */
public interface PricingStrategy {
    BigDecimal totalPrice(RoomType roomType, DateRange range);
}
