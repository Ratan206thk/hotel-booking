package com.rupeek.hotel.service;

import com.rupeek.hotel.domain.Owner;
import com.rupeek.hotel.exception.NotFoundException;
import com.rupeek.hotel.exception.ValidationException;
import com.rupeek.hotel.repository.OwnerRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OwnerService {

    private final OwnerRepository ownerRepository;

    public OwnerService(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }

    public Owner register(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Owner name is required");
        }
        return ownerRepository.save(new Owner(UUID.randomUUID().toString(), name.trim()));
    }

    public Owner get(String ownerId) {
        return ownerRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Owner not found: " + ownerId));
    }

    public void attachProperty(String ownerId, String propertyId) {
        Owner owner = get(ownerId);
        owner.addProperty(propertyId);
        ownerRepository.save(owner);
    }
}
