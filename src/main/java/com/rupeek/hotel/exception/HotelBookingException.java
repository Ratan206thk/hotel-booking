package com.rupeek.hotel.exception;

public abstract class HotelBookingException extends RuntimeException {
    protected HotelBookingException(String message) {
        super(message);
    }
}
