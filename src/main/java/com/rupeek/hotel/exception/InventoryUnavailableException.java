package com.rupeek.hotel.exception;

public class InventoryUnavailableException extends HotelBookingException {
    public InventoryUnavailableException(String message) {
        super(message);
    }
}
