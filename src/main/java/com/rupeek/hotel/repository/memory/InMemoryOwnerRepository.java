package com.rupeek.hotel.repository.memory;

import com.rupeek.hotel.domain.Owner;
import com.rupeek.hotel.repository.OwnerRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryOwnerRepository implements OwnerRepository {
    private final Map<String, Owner> store = new ConcurrentHashMap<>();

    @Override
    public Owner save(Owner owner) {
        store.put(owner.id(), owner);
        return owner;
    }

    @Override
    public Optional<Owner> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }
}
