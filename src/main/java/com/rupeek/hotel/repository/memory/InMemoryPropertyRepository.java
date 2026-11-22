package com.rupeek.hotel.repository.memory;

import com.rupeek.hotel.domain.Property;
import com.rupeek.hotel.repository.PropertyRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryPropertyRepository implements PropertyRepository {
    private final Map<String, Property> store = new ConcurrentHashMap<>();

    @Override
    public Property save(Property property) {
        store.put(property.id(), property);
        return property;
    }

    @Override
    public Optional<Property> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Property> findByCity(String city) {
        return store.values().stream()
                .filter(p -> p.location().city().equalsIgnoreCase(city.trim()))
                .toList();
    }
}
