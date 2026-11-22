package com.rupeek.hotel.service;

import com.rupeek.hotel.domain.DateRange;

public record BookingCommand(String guestId, String propertyId, String roomTypeId, DateRange range, int guests) {
}
