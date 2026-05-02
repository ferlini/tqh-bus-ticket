package com.tqh.bus.ticket.controller;

import com.tqh.bus.ticket.common.BusinessException;
import com.tqh.bus.ticket.common.ResultWrapper;
import com.tqh.bus.ticket.config.TqhProperties;
import com.tqh.bus.ticket.service.DateRangeCalculator;
import com.tqh.bus.ticket.service.TicketMonitorService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/monitor")
public class MonitorController {

    private final TicketMonitorService monitorService;
    private final DateRangeCalculator dateRangeCalculator;
    private final TqhProperties properties;

    public MonitorController(TicketMonitorService monitorService,
                             DateRangeCalculator dateRangeCalculator,
                             TqhProperties properties) {
        this.monitorService = monitorService;
        this.dateRangeCalculator = dateRangeCalculator;
        this.properties = properties;
    }

    @PostMapping("/this-week")
    public ResultWrapper<Map<String, Object>> monitorThisWeek() {
        List<LocalDate> dates = dateRangeCalculator.thisWeekDates(LocalDate.now());
        monitorService.startMonitor(dates);
        return ResultWrapper.success(buildData(dates));
    }

    @PostMapping("/next-week")
    public ResultWrapper<Map<String, Object>> monitorNextWeek() {
        List<LocalDate> dates = dateRangeCalculator.nextWeekDates(LocalDate.now());
        monitorService.startMonitor(dates);
        return ResultWrapper.success(buildData(dates));
    }

    @PostMapping("/dates")
    public ResultWrapper<Map<String, Object>> monitorDates(@RequestBody MonitorDatesRequest request) {
        List<LocalDate> parsedDates = parseDates(request.getDates());
        List<LocalDate> dates = dateRangeCalculator.filterPastDates(parsedDates, LocalDate.now());
        monitorService.startMonitor(dates);
        return ResultWrapper.success(buildData(dates));
    }

    @PostMapping("/watch/this-week")
    public ResultWrapper<Map<String, Object>> watchThisWeek() {
        List<LocalDate> dates = dateRangeCalculator.thisWeekDates(LocalDate.now());
        monitorService.startWatch(dates);
        return ResultWrapper.success(buildData(dates));
    }

    @PostMapping("/watch/next-week")
    public ResultWrapper<Map<String, Object>> watchNextWeek() {
        List<LocalDate> dates = dateRangeCalculator.nextWeekDates(LocalDate.now());
        monitorService.startWatch(dates);
        return ResultWrapper.success(buildData(dates));
    }

    @PostMapping("/watch/dates")
    public ResultWrapper<Map<String, Object>> watchDates(@RequestBody MonitorDatesRequest request) {
        List<LocalDate> parsedDates = parseDates(request.getDates());
        List<LocalDate> dates = dateRangeCalculator.filterPastDates(parsedDates, LocalDate.now());
        monitorService.startWatch(dates);
        return ResultWrapper.success(buildData(dates));
    }

    private List<LocalDate> parseDates(List<String> dateStrings) {
        try {
            return dateStrings.stream()
                    .map(LocalDate::parse)
                    .toList();
        } catch (DateTimeParseException e) {
            throw new BusinessException(400, "日期格式无效，请使用 yyyy-MM-dd");
        }
    }

    private Map<String, Object> buildData(List<LocalDate> dates) {
        return Map.of(
                "status", "started",
                "monitorDates", dates.stream().map(LocalDate::toString).toList(),
                "interval", properties.getMonitorInterval()
        );
    }
}
