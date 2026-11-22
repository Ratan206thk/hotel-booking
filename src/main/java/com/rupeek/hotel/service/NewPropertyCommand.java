package com.rupeek.hotel.service;

import com.rupeek.hotel.domain.Amenity;
import java.util.List;
import java.util.Set;

public record NewPropertyCommand(String name, String city, String locality, int starRating,
                                 Set<Amenity> amenities, List<NewRoomTypeCommand> roomTypes) {
}
