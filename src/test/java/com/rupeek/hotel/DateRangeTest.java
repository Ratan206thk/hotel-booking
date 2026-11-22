package com.rupeek.hotel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.rupeek.hotel.domain.DateRange;
import com.rupeek.hotel.exception.ValidationException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DateRangeTest {

    @Test
    void countsNightsAndListsEachOccupiedNight() {
        DateRange range = TestContext.stay("2026-12-10", "2026-12-13");
        assertEquals(3, range.nights());
        assertEquals(java.util.List.of(LocalDate.parse("2026-12-10"), LocalDate.parse("2026-12-11"),
                LocalDate.parse("2026-12-12")), range.nightDates());
    }

    @Test
    void rejectsZeroOrNegativeLengthStays() {
        assertThrows(ValidationException.class, () -> TestContext.stay("2026-12-10", "2026-12-10"));
        assertThrows(ValidationException.class, () -> TestContext.stay("2026-12-10", "2026-12-09"));
    }

    @Test
    void rejectsMissingDates() {
        assertThrows(ValidationException.class, () -> new DateRange(null, LocalDate.parse("2026-12-10")));
    }
}
