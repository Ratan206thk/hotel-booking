package com.rupeek.hotel.search;

import com.rupeek.hotel.domain.Property;
import java.util.List;

public record PropertySearchResult(Property property, List<RoomOffer> offers) {
}
