package com.rupeek.hotel.service;

import com.rupeek.hotel.domain.Location;
import com.rupeek.hotel.domain.Owner;
import com.rupeek.hotel.domain.Property;
import com.rupeek.hotel.domain.RoomType;
import com.rupeek.hotel.exception.NotFoundException;
import com.rupeek.hotel.repository.PropertyRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PropertyService {

    private final OwnerService ownerService;
    private final PropertyRepository propertyRepository;

    public PropertyService(OwnerService ownerService, PropertyRepository propertyRepository) {
        this.ownerService = ownerService;
        this.propertyRepository = propertyRepository;
    }

    /** Adds a property to an existing owner account (a chain adds several). */
    public Property addProperty(String ownerId, NewPropertyCommand command) {
        Owner owner = ownerService.get(ownerId);
        List<RoomType> roomTypes = command.roomTypes() == null ? List.of() : command.roomTypes().stream()
                .map(r -> new RoomType(newId(), r.name(), r.capacity(), r.pricePerNight(), r.totalRooms()))
                .toList();
        Property property = new Property(newId(), owner.id(), command.name(),
                new Location(command.city(), command.locality()), command.starRating(),
                command.amenities(), roomTypes);
        propertyRepository.save(property);
        ownerService.attachProperty(owner.id(), property.id());
        return property;
    }

    /** A standalone property is just an owner account with exactly one property. */
    public Property onboardStandalone(NewPropertyCommand command) {
        Owner owner = ownerService.register(command.name());
        return addProperty(owner.id(), command);
    }

    public Property get(String propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new NotFoundException("Property not found: " + propertyId));
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }
}
