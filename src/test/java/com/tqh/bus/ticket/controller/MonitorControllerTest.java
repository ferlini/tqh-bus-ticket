package com.tqh.bus.ticket.controller;

import com.tqh.bus.ticket.common.GlobalExceptionHandler;
import com.tqh.bus.ticket.config.TqhProperties;
import com.tqh.bus.ticket.service.DateRangeCalculator;
import com.tqh.bus.ticket.service.TicketMonitorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MonitorController.class)
@Import(GlobalExceptionHandler.class)
class MonitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketMonitorService monitorService;

    @MockitoBean
    private DateRangeCalculator dateRangeCalculator;

    @MockitoBean
    private TqhProperties tqhProperties;

    @Test
    void should_return_this_week_dates_when_post_this_week() throws Exception {
        // given
        given(tqhProperties.getMonitorInterval()).willReturn(30);
        given(dateRangeCalculator.thisWeekDates(LocalDate.now()))
                .willReturn(List.of(
                        LocalDate.of(2026, 3, 25),
                        LocalDate.of(2026, 3, 26),
                        LocalDate.of(2026, 3, 27),
                        LocalDate.of(2026, 3, 28),
                        LocalDate.of(2026, 3, 29)
                ));

        // when & then
        mockMvc.perform(post("/monitor/this-week"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("started"))
                .andExpect(jsonPath("$.data.monitorDates", hasSize(5)))
                .andExpect(jsonPath("$.data.interval").value(30));

        verify(monitorService).startMonitor(anyList());
    }

    @Test
    void should_return_next_week_dates_when_post_next_week() throws Exception {
        // given
        given(tqhProperties.getMonitorInterval()).willReturn(30);
        given(dateRangeCalculator.nextWeekDates(LocalDate.now()))
                .willReturn(List.of(
                        LocalDate.of(2026, 3, 30),
                        LocalDate.of(2026, 3, 31),
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 4, 2),
                        LocalDate.of(2026, 4, 3),
                        LocalDate.of(2026, 4, 4),
                        LocalDate.of(2026, 4, 5)
                ));

        // when & then
        mockMvc.perform(post("/monitor/next-week"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("started"))
                .andExpect(jsonPath("$.data.monitorDates", hasSize(7)))
                .andExpect(jsonPath("$.data.interval").value(30));

        verify(monitorService).startMonitor(anyList());
    }

    @Test
    void should_accept_custom_dates_when_post_dates() throws Exception {
        // given
        given(tqhProperties.getMonitorInterval()).willReturn(30);
        given(dateRangeCalculator.filterPastDates(anyList(), org.mockito.ArgumentMatchers.any(LocalDate.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when & then
        mockMvc.perform(post("/monitor/dates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dates\": [\"2026-03-25\", \"2026-03-26\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.monitorDates", hasSize(2)))
                .andExpect(jsonPath("$.data.monitorDates[0]").value("2026-03-25"))
                .andExpect(jsonPath("$.data.monitorDates[1]").value("2026-03-26"))
                .andExpect(jsonPath("$.data.interval").value(30));

        verify(monitorService).startMonitor(anyList());
    }

    @Test
    void should_reject_invalid_date_format() throws Exception {
        mockMvc.perform(post("/monitor/dates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dates\": [\"not-a-date\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("日期格式无效，请使用 yyyy-MM-dd"));
    }

    @Test
    void should_filter_past_dates_from_request() throws Exception {
        // given
        given(tqhProperties.getMonitorInterval()).willReturn(30);
        given(dateRangeCalculator.filterPastDates(anyList(), org.mockito.ArgumentMatchers.any(LocalDate.class)))
                .willReturn(List.of(LocalDate.of(2026, 3, 26)));

        // when & then
        mockMvc.perform(post("/monitor/dates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dates\": [\"2026-03-20\", \"2026-03-26\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.monitorDates", hasSize(1)))
                .andExpect(jsonPath("$.data.monitorDates[0]").value("2026-03-26"));
    }
}
