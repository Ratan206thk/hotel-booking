package com.rupeek.hotel.exception;

public class InvalidStateException extends HotelBookingException {
    public InvalidStateException(String message) {
        super(message);
    }
}
