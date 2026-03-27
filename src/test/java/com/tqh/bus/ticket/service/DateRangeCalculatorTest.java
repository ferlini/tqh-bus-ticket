package com.tqh.bus.ticket.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DateRangeCalculatorTest {

    private final DateRangeCalculator calculator = new DateRangeCalculator();

    // === thisWeekDates ===

    @Test
    void should_return_wednesday_to_sunday_when_today_is_wednesday() {
        // 2026-03-25 is Wednesday
        LocalDate wednesday = LocalDate.of(2026, 3, 25);

        List<LocalDate> dates = calculator.thisWeekDates(wednesday);

        assertThat(dates).containsExactly(
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 26),
                LocalDate.of(2026, 3, 27),
                LocalDate.of(2026, 3, 28),
                LocalDate.of(2026, 3, 29)
        );
    }

    @Test
    void should_return_monday_to_sunday_when_today_is_monday() {
        // 2026-03-23 is Monday
        LocalDate monday = LocalDate.of(2026, 3, 23);

        List<LocalDate> dates = calculator.thisWeekDates(monday);

        assertThat(dates).hasSize(7);
        assertThat(dates.get(0)).isEqualTo(LocalDate.of(2026, 3, 23));
        assertThat(dates.get(6)).isEqualTo(LocalDate.of(2026, 3, 29));
    }

    @Test
    void should_return_only_sunday_when_today_is_sunday() {
        // 2026-03-29 is Sunday
        LocalDate sunday = LocalDate.of(2026, 3, 29);

        List<LocalDate> dates = calculator.thisWeekDates(sunday);

        assertThat(dates).containsExactly(LocalDate.of(2026, 3, 29));
    }

    // === nextWeekDates ===

    @Test
    void should_return_next_monday_to_sunday() {
        // 2026-03-25 is Wednesday, next week is 03-30 ~ 04-05
        LocalDate wednesday = LocalDate.of(2026, 3, 25);

        List<LocalDate> dates = calculator.nextWeekDates(wednesday);

        assertThat(dates).hasSize(7);
        assertThat(dates.get(0)).isEqualTo(LocalDate.of(2026, 3, 30));
        assertThat(dates.get(6)).isEqualTo(LocalDate.of(2026, 4, 5));
    }

    // === filterPastDates ===

    @Test
    void should_exclude_past_dates() {
        LocalDate today = LocalDate.of(2026, 3, 25);
        List<LocalDate> dates = List.of(
                LocalDate.of(2026, 3, 23),
                LocalDate.of(2026, 3, 24),
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 26)
        );

        List<LocalDate> filtered = calculator.filterPastDates(dates, today);

        assertThat(filtered).containsExactly(
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 26)
        );
    }

    @Test
    void should_return_empty_when_all_dates_past() {
        LocalDate today = LocalDate.of(2026, 3, 25);
        List<LocalDate> dates = List.of(
                LocalDate.of(2026, 3, 23),
                LocalDate.of(2026, 3, 24)
        );

        List<LocalDate> filtered = calculator.filterPastDates(dates, today);

        assertThat(filtered).isEmpty();
    }
}
