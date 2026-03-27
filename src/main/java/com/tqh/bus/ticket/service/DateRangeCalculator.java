package com.tqh.bus.ticket.service;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Stream;

@Component
public class DateRangeCalculator {

    public List<LocalDate> thisWeekDates(LocalDate today) {
        LocalDate sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return today.datesUntil(sunday.plusDays(1)).toList();
    }

    public List<LocalDate> nextWeekDates(LocalDate today) {
        LocalDate nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        LocalDate nextSunday = nextMonday.plusDays(6);
        return nextMonday.datesUntil(nextSunday.plusDays(1)).toList();
    }

    public List<LocalDate> filterPastDates(List<LocalDate> dates, LocalDate today) {
        return dates.stream()
                .filter(date -> !date.isBefore(today))
                .toList();
    }
}
