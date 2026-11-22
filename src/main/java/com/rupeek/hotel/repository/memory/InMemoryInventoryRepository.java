package com.rupeek.hotel.repository.memory;

import com.rupeek.hotel.domain.DateRange;
import com.rupeek.hotel.repository.InventoryKey;
import com.rupeek.hotel.repository.InventoryRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * Per-pool locking: bookings on different room types never contend, while check-and-reserve on the same
 * pool is atomic. (A SQL implementation would use a row lock / conditional UPDATE instead.)
 */
@Repository
public class InMemoryInventoryRepository implements InventoryRepository {

    private final Map<InventoryKey, Object> locks = new ConcurrentHashMap<>();
    private final Map<InventoryKey, Map<LocalDate, Integer>> heldRooms = new ConcurrentHashMap<>();

    @Override
    public int availableRooms(InventoryKey key, DateRange range, int totalRooms) {
        synchronized (lockFor(key)) {
            return totalRooms - peakHeld(key, range);
        }
    }

    @Override
    public boolean tryReserve(InventoryKey key, DateRange range, int totalRooms) {
        synchronized (lockFor(key)) {
            if (totalRooms - peakHeld(key, range) < 1) {
                return false;
            }
            Map<LocalDate, Integer> held = heldRooms.computeIfAbsent(key, k -> new HashMap<>());
            range.nightDates().forEach(night -> held.merge(night, 1, Integer::sum));
            return true;
        }
    }

    @Override
    public void release(InventoryKey key, DateRange range) {
        synchronized (lockFor(key)) {
            Map<LocalDate, Integer> held = heldRooms.get(key);
            if (held == null) {
                return;
            }
            range.nightDates().forEach(night -> held.computeIfPresent(night, (d, count) -> count > 1 ? count - 1 : null));
        }
    }

    private int peakHeld(InventoryKey key, DateRange range) {
        Map<LocalDate, Integer> held = heldRooms.get(key);
        if (held == null) {
            return 0;
        }
        return range.nightDates().stream().mapToInt(night -> held.getOrDefault(night, 0)).max().orElse(0);
    }

    private Object lockFor(InventoryKey key) {
        return locks.computeIfAbsent(key, k -> new Object());
    }
}
