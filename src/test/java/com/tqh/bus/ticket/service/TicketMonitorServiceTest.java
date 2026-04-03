package com.tqh.bus.ticket.service;

import com.tqh.bus.ticket.common.BusinessException;
import com.tqh.bus.ticket.common.UnpaidOrderException;
import com.tqh.bus.ticket.config.TqhProperties;
import com.tqh.bus.ticket.integration.TqhApiClient;
import com.tqh.bus.ticket.integration.model.CreateOrderResponse;
import com.tqh.bus.ticket.integration.model.ScheduleItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketMonitorServiceTest {

    @Mock
    private TqhApiClient apiClient;

    @Mock
    private OrderService orderService;

    @Mock
    private TicketLogService ticketLogService;

    @Mock
    private TqhProperties properties;

    @InjectMocks
    private TicketMonitorService monitorService;

    // === executeMonitorCycle (3.4.1) ===

    @Test
    void should_intersect_target_dates_with_available_schedules() {
        // given: target dates are 03-25, 03-26, 03-27; API returns 03-25, 03-26 only
        given(properties.getRouteId()).willReturn(275);

        ScheduleItem item25 = createSchedule(61430, "2026/3/25", 1);
        ScheduleItem item26 = createSchedule(61431, "2026/3/26", 1);
        given(apiClient.findSchedules(275))
                .willReturn(Map.of("07:40", List.of(item25, item26)));

        given(orderService.findPaidOrderDates(eq(275), anyInt(), any())).willReturn(Set.of());
        given(orderService.tryCreateOrder(any())).willReturn(true);

        List<LocalDate> targetDates = List.of(
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 26),
                LocalDate.of(2026, 3, 27)
        );

        // when
        monitorService.executeMonitorCycle(targetDates);

        // then: only 2 dates should be processed (03-25 and 03-26)
        verify(orderService, times(2)).tryCreateOrder(any());
    }

    @Test
    void should_skip_dates_with_zero_tickets() {
        // given
        given(properties.getRouteId()).willReturn(275);

        ScheduleItem withTickets = createSchedule(61429, "2026/3/25", 1);
        ScheduleItem noTickets = createSchedule(61430, "2026/3/26", 0);
        given(apiClient.findSchedules(275))
                .willReturn(Map.of("07:40", List.of(withTickets, noTickets)));

        given(orderService.findPaidOrderDates(eq(275), anyInt(), any())).willReturn(Set.of());
        given(orderService.tryCreateOrder(any())).willReturn(true);

        List<LocalDate> targetDates = List.of(
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 26)
        );

        // when
        monitorService.executeMonitorCycle(targetDates);

        // then: only 03-25 with number > 0
        verify(orderService, times(1)).tryCreateOrder(any());
    }

    @Test
    void should_call_order_service_for_each_available_date() {
        // given
        given(properties.getRouteId()).willReturn(275);

        ScheduleItem item25 = createSchedule(61429, "2026/3/25", 2);
        ScheduleItem item26 = createSchedule(61430, "2026/3/26", 3);
        given(apiClient.findSchedules(275))
                .willReturn(Map.of("07:40", List.of(item25, item26)));

        given(orderService.findPaidOrderDates(eq(275), anyInt(), any())).willReturn(Set.of());
        given(orderService.tryCreateOrder(any())).willReturn(true);

        List<LocalDate> targetDates = List.of(
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 26)
        );

        // when
        monitorService.executeMonitorCycle(targetDates);

        // then
        verify(orderService).tryCreateOrder(item25);
        verify(orderService).tryCreateOrder(item26);
    }

    @Test
    void should_continue_processing_next_date_when_one_fails() {
        // given
        given(properties.getRouteId()).willReturn(275);

        ScheduleItem item25 = createSchedule(61429, "2026/3/25", 1);
        ScheduleItem item26 = createSchedule(61430, "2026/3/26", 1);
        given(apiClient.findSchedules(275))
                .willReturn(Map.of("07:40", List.of(item25, item26)));

        given(orderService.findPaidOrderDates(eq(275), anyInt(), any())).willReturn(Set.of());
        given(orderService.tryCreateOrder(item25)).willThrow(new BusinessException("网络超时"));
        given(orderService.tryCreateOrder(item26)).willReturn(true);

        List<LocalDate> targetDates = List.of(
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 26)
        );

        // when
        monitorService.executeMonitorCycle(targetDates);

        // then: item26 should still be processed
        verify(orderService).tryCreateOrder(item26);
    }

    @Test
    void should_log_ticket_after_successful_order() {
        // given
        given(properties.getRouteId()).willReturn(275);

        ScheduleItem item25 = createSchedule(61429, "2026/3/25", 1);
        given(apiClient.findSchedules(275))
                .willReturn(Map.of("07:40", List.of(item25)));

        given(orderService.findPaidOrderDates(eq(275), anyInt(), any())).willReturn(Set.of());
        given(orderService.tryCreateOrder(item25)).willReturn(true);
        given(orderService.getLastCreatedOrderId()).willReturn(572468);

        List<LocalDate> targetDates = List.of(LocalDate.of(2026, 3, 25));

        // when
        monitorService.executeMonitorCycle(targetDates);

        // then
        verify(ticketLogService).logTicketPurchase(572468);
    }

    @Test
    void should_not_log_ticket_when_order_skipped() {
        // given
        given(properties.getRouteId()).willReturn(275);

        ScheduleItem item25 = createSchedule(61429, "2026/3/25", 1);
        given(apiClient.findSchedules(275))
                .willReturn(Map.of("07:40", List.of(item25)));

        given(orderService.findPaidOrderDates(eq(275), anyInt(), any())).willReturn(Set.of());
        given(orderService.tryCreateOrder(item25)).willReturn(false);

        List<LocalDate> targetDates = List.of(LocalDate.of(2026, 3, 25));

        // when
        monitorService.executeMonitorCycle(targetDates);

        // then
        verify(ticketLogService, never()).logTicketPurchase(anyInt());
    }

    @Test
    void should_stop_monitor_when_unpaid_order_found() {
        // given
        given(properties.getRouteId()).willReturn(275);

        ScheduleItem item25 = createSchedule(61429, "2026/3/25", 1);
        given(apiClient.findSchedules(275))
                .willReturn(Map.of("07:40", List.of(item25)));

        given(orderService.findPaidOrderDates(eq(275), anyInt(), any()))
                .willThrow(new UnpaidOrderException("你有待支付的订单"));

        List<LocalDate> targetDates = List.of(LocalDate.of(2026, 3, 25));

        // when
        monitorService.executeMonitorCycle(targetDates);

        // then: no order attempts, monitor should stop
        verify(orderService, never()).tryCreateOrder(any());
        verify(ticketLogService).writeUnpaidOrderWarning("你有待支付的订单");
    }

    // === paid order pre-check ===

    @Test
    void should_exclude_dates_with_paid_orders_from_monitoring() {
        // given: 03-25 has paid order, 03-26 does not
        given(properties.getRouteId()).willReturn(275);

        ScheduleItem item25 = createSchedule(61430, "2026/3/25", 1);
        ScheduleItem item26 = createSchedule(61431, "2026/3/26", 1);
        given(apiClient.findSchedules(275))
                .willReturn(Map.of("07:40", List.of(item25, item26)));

        given(orderService.findPaidOrderDates(eq(275), anyInt(), any()))
                .willReturn(Set.of(LocalDate.of(2026, 3, 25)));

        given(orderService.tryCreateOrder(any())).willReturn(true);

        List<LocalDate> targetDates = List.of(
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 26)
        );

        // when
        monitorService.executeMonitorCycle(targetDates);

        // then: only 03-26 should be processed, 03-25 excluded
        verify(orderService, never()).tryCreateOrder(item25);
        verify(orderService).tryCreateOrder(item26);
    }

    @Test
    void should_skip_monitoring_entirely_when_all_dates_have_paid_orders() {
        // given
        given(properties.getRouteId()).willReturn(275);

        ScheduleItem item25 = createSchedule(61430, "2026/3/25", 1);
        given(apiClient.findSchedules(275))
                .willReturn(Map.of("07:40", List.of(item25)));

        given(orderService.findPaidOrderDates(eq(275), anyInt(), any()))
                .willReturn(Set.of(LocalDate.of(2026, 3, 25)));

        List<LocalDate> targetDates = List.of(LocalDate.of(2026, 3, 25));

        // when
        monitorService.executeMonitorCycle(targetDates);

        // then: no order attempts
        verify(orderService, never()).tryCreateOrder(any());
    }

    @Test
    void should_proceed_normally_when_no_paid_orders() {
        // given
        given(properties.getRouteId()).willReturn(275);

        ScheduleItem item25 = createSchedule(61430, "2026/3/25", 1);
        given(apiClient.findSchedules(275))
                .willReturn(Map.of("07:40", List.of(item25)));

        given(orderService.findPaidOrderDates(eq(275), anyInt(), any()))
                .willReturn(Set.of());

        given(orderService.tryCreateOrder(any())).willReturn(true);

        List<LocalDate> targetDates = List.of(LocalDate.of(2026, 3, 25));

        // when
        monitorService.executeMonitorCycle(targetDates);

        // then
        verify(orderService).tryCreateOrder(item25);
    }

    // === startMonitor / stopMonitor (3.4.3) ===

    @Test
    void should_continue_monitoring_when_api_fails() {
        // given
        given(properties.getRouteId()).willReturn(275);
        given(apiClient.findSchedules(275)).willThrow(new BusinessException("网络超时"));

        List<LocalDate> targetDates = List.of(LocalDate.of(2026, 3, 25));

        // when & then: should not throw
        monitorService.executeMonitorCycle(targetDates);
    }

    private ScheduleItem createSchedule(int id, String date, int number) {
        ScheduleItem item = new ScheduleItem();
        item.setId(id);
        item.setDate(date);
        item.setNumber(number);
        return item;
    }
}
