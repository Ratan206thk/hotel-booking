package com.rupeek.hotel.repository;

import com.rupeek.hotel.domain.DateRange;

/**
 * Tracks how many rooms of a pool are held on each night. {@code tryReserve} is an atomic
 * check-and-reserve so concurrent bookings can never oversell a pool.
 */
public interface InventoryRepository {

    /** Rooms free on every night of the range (i.e. the minimum free count across the stay). */
    int availableRooms(InventoryKey key, DateRange range, int totalRooms);

    /** Atomically reserves one room for each night in the range; false when the pool is sold out on any night. */
    boolean tryReserve(InventoryKey key, DateRange range, int totalRooms);

    /** Returns one room per night of the range to the pool. */
    void release(InventoryKey key, DateRange range);
}
