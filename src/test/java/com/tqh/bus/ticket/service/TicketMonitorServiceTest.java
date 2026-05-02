package com.tqh.bus.ticket.service;

import com.tqh.bus.ticket.common.BusinessException;
import com.tqh.bus.ticket.common.UnpaidOrderException;
import com.tqh.bus.ticket.config.TqhProperties;
import com.tqh.bus.ticket.integration.OpenClawWebhookClient;
import com.tqh.bus.ticket.integration.TqhApiClient;
import com.tqh.bus.ticket.integration.model.CreateOrderResponse;
import com.tqh.bus.ticket.integration.model.RouteStopsResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
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

    @Mock
    private OpenClawWebhookClient openClawWebhookClient;

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
    void should_send_webhook_with_purchase_message_after_successful_order() {
        // given
        given(properties.getRouteId()).willReturn(275);

        ScheduleItem item25 = createSchedule(61429, "2026/3/25", 1);
        given(apiClient.findSchedules(275))
                .willReturn(Map.of("07:40", List.of(item25)));

        given(orderService.findPaidOrderDates(eq(275), anyInt(), any())).willReturn(Set.of());
        given(orderService.tryCreateOrder(item25)).willReturn(true);
        given(orderService.getLastCreatedOrderId()).willReturn(572468);
        given(ticketLogService.logTicketPurchase(572468))
                .willReturn("----------------------------------------\n日期: 2026/3/25\n");

        List<LocalDate> targetDates = List.of(LocalDate.of(2026, 3, 25));

        // when
        monitorService.executeMonitorCycle(targetDates);

        // then
        verify(openClawWebhookClient).notifyTicketPurchase(
                "----------------------------------------\n日期: 2026/3/25\n");
    }

    @Test
    void should_not_send_webhook_when_order_skipped() {
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
        verify(openClawWebhookClient, never()).notifyTicketPurchase(any());
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

    @Test
    void should_skip_paid_order_check_when_no_available_schedules() {
        // given
        given(properties.getRouteId()).willReturn(275);

        ScheduleItem noTickets = createSchedule(61430, "2026/3/25", 0);
        given(apiClient.findSchedules(275))
                .willReturn(Map.of("07:40", List.of(noTickets)));

        List<LocalDate> targetDates = List.of(LocalDate.of(2026, 3, 25));

        // when
        monitorService.executeMonitorCycle(targetDates);

        // then: no paid order check, no order attempts
        verify(orderService, never()).findPaidOrderDates(anyInt(), anyInt(), any());
        verify(orderService, never()).tryCreateOrder(any());
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

    // === executeWatchCycle (watch-only mode) ===

    @Test
    void should_send_availability_webhook_when_tickets_found_in_watch_cycle() {
        // given
        given(properties.getRouteId()).willReturn(275);
        ScheduleItem item25 = createSchedule(61429, "2026/3/25", 10);
        given(apiClient.findSchedules(275))
                .willReturn(Map.of("07:40", List.of(item25)));
        given(apiClient.getRouteStops(eq(275), any())).willReturn(routeStops("线路A"));
        given(openClawWebhookClient.notifyTicketAvailable(anyString())).willReturn(true);

        // when
        boolean shouldStop = monitorService.executeWatchCycle(List.of(LocalDate.of(2026, 3, 25)));

        // then
        assertThat(shouldStop).isTrue();
        verify(openClawWebhookClient).notifyTicketAvailable(
                "线路A\n  - 2026-03-25: 剩余10张");
        verify(orderService, never()).tryCreateOrder(any());
    }

    @Test
    void should_merge_multiple_dates_into_single_availability_message() {
        // given
        given(properties.getRouteId()).willReturn(275);
        ScheduleItem item02 = createSchedule(61429, "2026/5/2", 10);
        ScheduleItem item03 = createSchedule(61430, "2026/5/3", 5);
        ScheduleItem item04 = createSchedule(61431, "2026/5/4", 3);
        given(apiClient.findSchedules(275))
                .willReturn(Map.of("07:40", List.of(item04, item02, item03)));
        given(apiClient.getRouteStops(eq(275), any())).willReturn(routeStops("线路A"));
        given(openClawWebhookClient.notifyTicketAvailable(anyString())).willReturn(true);

        // when
        monitorService.executeWatchCycle(List.of(
                LocalDate.of(2026, 5, 2),
                LocalDate.of(2026, 5, 3),
                LocalDate.of(2026, 5, 4)));

        // then: dates merged in ascending order, single notify call
        verify(openClawWebhookClient).notifyTicketAvailable(
                "线路A\n  - 2026-05-02: 剩余10张\n  - 2026-05-03: 剩余5张\n  - 2026-05-04: 剩余3张");
    }

    @Test
    void should_return_false_and_skip_webhook_when_no_tickets_in_watch_cycle() {
        // given
        given(properties.getRouteId()).willReturn(275);
        ScheduleItem noTickets = createSchedule(61430, "2026/3/25", 0);
        given(apiClient.findSchedules(275))
                .willReturn(Map.of("07:40", List.of(noTickets)));

        // when
        boolean shouldStop = monitorService.executeWatchCycle(List.of(LocalDate.of(2026, 3, 25)));

        // then
        assertThat(shouldStop).isFalse();
        verify(apiClient, never()).getRouteStops(anyInt(), any());
        verify(openClawWebhookClient, never()).notifyTicketAvailable(anyString());
    }

    @Test
    void should_return_false_when_availability_webhook_fails() {
        // given
        given(properties.getRouteId()).willReturn(275);
        ScheduleItem item25 = createSchedule(61429, "2026/3/25", 10);
        given(apiClient.findSchedules(275))
                .willReturn(Map.of("07:40", List.of(item25)));
        given(apiClient.getRouteStops(eq(275), any())).willReturn(routeStops("线路A"));
        given(openClawWebhookClient.notifyTicketAvailable(anyString())).willReturn(false);

        // when
        boolean shouldStop = monitorService.executeWatchCycle(List.of(LocalDate.of(2026, 3, 25)));

        // then: failure means scheduler keeps retrying next cycle
        assertThat(shouldStop).isFalse();
    }

    @Test
    void should_return_false_when_watch_cycle_throws_exception() {
        // given
        given(properties.getRouteId()).willReturn(275);
        given(apiClient.findSchedules(275)).willThrow(new BusinessException("网络超时"));

        // when
        boolean shouldStop = monitorService.executeWatchCycle(List.of(LocalDate.of(2026, 3, 25)));

        // then: error must not propagate (would kill the scheduler)
        assertThat(shouldStop).isFalse();
    }

    private RouteStopsResponse routeStops(String routeName) {
        RouteStopsResponse stops = new RouteStopsResponse();
        stops.setRouteName(routeName);
        return stops;
    }

    private ScheduleItem createSchedule(int id, String date, int number) {
        ScheduleItem item = new ScheduleItem();
        item.setId(id);
        item.setDate(date);
        item.setNumber(number);
        return item;
    }
}
