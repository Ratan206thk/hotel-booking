package com.rupeek.hotel.search;

import com.rupeek.hotel.domain.Property;
import com.rupeek.hotel.domain.RoomType;
import com.rupeek.hotel.pricing.PricingStrategy;
import com.rupeek.hotel.repository.InventoryKey;
import com.rupeek.hotel.repository.InventoryRepository;
import com.rupeek.hotel.repository.PropertyRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    private final PropertyRepository propertyRepository;
    private final InventoryRepository inventoryRepository;
    private final PricingStrategy pricingStrategy;
    private final List<PropertyFilterFactory> filterFactories;

    public SearchService(PropertyRepository propertyRepository, InventoryRepository inventoryRepository,
                         PricingStrategy pricingStrategy, List<PropertyFilterFactory> filterFactories) {
        this.propertyRepository = propertyRepository;
        this.inventoryRepository = inventoryRepository;
        this.pricingStrategy = pricingStrategy;
        this.filterFactories = filterFactories;
    }

    /** Properties in the city with at least one bookable room for the dates and party size, passing all filters. */
    public List<PropertySearchResult> search(SearchCriteria criteria) {
        List<PropertyFilter> filters = filterFactories.stream()
                .map(factory -> factory.create(criteria.filters()))
                .flatMap(Optional::stream)
                .toList();

        return propertyRepository.findByCity(criteria.city()).stream()
                .map(property -> new PropertySearchResult(property, bookableOffers(property, criteria)))
                .filter(result -> !result.offers().isEmpty())
                .filter(result -> filters.stream().allMatch(f -> f.matches(result)))
                .toList();
    }

    private List<RoomOffer> bookableOffers(Property property, SearchCriteria criteria) {
        List<RoomOffer> offers = new ArrayList<>();
        for (RoomType roomType : property.roomTypes()) {
            if (roomType.capacity() < criteria.guests()) {
                continue;
            }
            int available = inventoryRepository.availableRooms(
                    new InventoryKey(property.id(), roomType.id()), criteria.range(), roomType.totalRooms());
            if (available > 0) {
                offers.add(new RoomOffer(roomType, available, pricingStrategy.totalPrice(roomType, criteria.range())));
            }
        }
        return offers;
    }
}
