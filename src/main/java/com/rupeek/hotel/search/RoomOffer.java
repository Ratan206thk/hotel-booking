package com.rupeek.hotel.search;

import com.rupeek.hotel.domain.RoomType;
import java.math.BigDecimal;

/** A room type that can actually be booked for the searched dates and party size. */
public record RoomOffer(RoomType roomType, int availableRooms, BigDecimal totalPrice) {
}
