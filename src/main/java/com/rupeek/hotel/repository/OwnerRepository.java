package com.rupeek.hotel.repository;

import com.rupeek.hotel.domain.Owner;
import java.util.Optional;

public interface OwnerRepository {
    Owner save(Owner owner);

    Optional<Owner> findById(String id);
}
