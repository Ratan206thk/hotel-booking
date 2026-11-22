package com.rupeek.hotel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rupeek.hotel.domain.DateRange;
import com.rupeek.hotel.repository.InventoryKey;
import com.rupeek.hotel.repository.memory.InMemoryInventoryRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InventoryRepositoryTest {

    private final InMemoryInventoryRepository inventory = new InMemoryInventoryRepository();
    private final InventoryKey key = new InventoryKey("p1", "r1");

    @Test
    void sellsOutAtCapacityForOverlappingDates() {
        DateRange stay = TestContext.stay("2026-12-10", "2026-12-12");
        assertTrue(inventory.tryReserve(key, stay, 2));
        assertTrue(inventory.tryReserve(key, TestContext.stay("2026-12-11", "2026-12-13"), 2));
        // 11th is now held twice, so any stay touching that night is full
        assertFalse(inventory.tryReserve(key, TestContext.stay("2026-12-11", "2026-12-12"), 2));
        assertEquals(0, inventory.availableRooms(key, TestContext.stay("2026-12-11", "2026-12-12"), 2));
    }

    @Test
    void backToBackStaysDoNotConflict() {
        assertTrue(inventory.tryReserve(key, TestContext.stay("2026-12-10", "2026-12-12"), 1));
        // check-out day of the first stay is check-in day of the second
        assertTrue(inventory.tryReserve(key, TestContext.stay("2026-12-12", "2026-12-14"), 1));
    }

    @Test
    void releaseMakesRoomsAvailableAgain() {
        DateRange stay = TestContext.stay("2026-12-10", "2026-12-12");
        assertTrue(inventory.tryReserve(key, stay, 1));
        assertFalse(inventory.tryReserve(key, stay, 1));
        inventory.release(key, stay);
        assertEquals(1, inventory.availableRooms(key, stay, 1));
        assertTrue(inventory.tryReserve(key, stay, 1));
    }

    @Test
    void differentRoomPoolsAreIndependent() {
        DateRange stay = TestContext.stay("2026-12-10", "2026-12-12");
        assertTrue(inventory.tryReserve(key, stay, 1));
        assertTrue(inventory.tryReserve(new InventoryKey("p1", "r2"), stay, 1));
    }

    @Test
    void concurrentReservationsNeverOversell() throws Exception {
        int rooms = 5;
        int threads = 50;
        DateRange stay = TestContext.stay("2026-12-10", "2026-12-12");
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                start.await();
                if (inventory.tryReserve(key, stay, rooms)) {
                    successes.incrementAndGet();
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(rooms, successes.get());
        assertEquals(0, inventory.availableRooms(key, stay, rooms));
    }
}
