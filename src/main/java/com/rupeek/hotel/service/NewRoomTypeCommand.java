package com.rupeek.hotel.service;

import java.math.BigDecimal;

public record NewRoomTypeCommand(String name, int capacity, BigDecimal pricePerNight, int totalRooms) {
}
