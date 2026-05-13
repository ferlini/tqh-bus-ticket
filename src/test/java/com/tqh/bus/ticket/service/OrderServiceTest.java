package com.tqh.bus.ticket.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tqh.bus.ticket.common.BusinessException;
import com.tqh.bus.ticket.common.UnpaidOrderException;
import com.tqh.bus.ticket.config.TqhProperties;
import com.tqh.bus.ticket.integration.TqhApiClient;
import com.tqh.bus.ticket.integration.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private TqhApiClient apiClient;

    @Mock
    private TqhProperties properties;

    @InjectMocks
    private OrderService orderService;

    // === findPaidOrderDates ===

    // === findUsableCoupons (3.2.3) ===

    @Test
    void should_filter_usable_coupons_by_is_use_and_status() {
        // given
        CouponItem usable = createCoupon(8317178, 2, Map.of("61429", true), Map.of("61429", "待使用"));
        CouponItem unusable = createCoupon(8190582, 2, Map.of("61429", false), Map.of("61429", "不可使用"));
        given(apiClient.getCoupons(275, List.of(61429), 24)).willReturn(List.of(usable, unusable));

        // when
        List<CouponItem> result = orderService.findUsableCoupons(275, 61429, 24);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(8317178);
    }

    @Test
    void should_return_empty_list_when_no_usable_coupons() {
        // given
        CouponItem unusable = createCoupon(8190582, 2, Map.of("61429", false), Map.of("61429", "不可使用"));
        given(apiClient.getCoupons(275, List.of(61429), 24)).willReturn(List.of(unusable));

        // when
        List<CouponItem> result = orderService.findUsableCoupons(275, 61429, 24);

        // then
        assertThat(result).isEmpty();
    }

    // === tryVerifyCoupon (3.2.5) ===

    @Test
    void should_return_first_verified_coupon() {
        // given
        CouponItem couponA = createCoupon(100, 2, Map.of(), Map.of());
        given(apiClient.verifyPrice(any())).willReturn(new PriceVerificationResponse());
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);

        // when
        Optional<CouponItem> result = orderService.tryVerifyCoupon(List.of(couponA), 61429);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(100);
    }

    @Test
    void should_use_second_coupon_when_first_verification_fails() {
        // given
        CouponItem couponA = createCoupon(100, 2, Map.of(), Map.of());
        CouponItem couponB = createCoupon(200, 2, Map.of(), Map.of());
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);
        given(apiClient.verifyPrice(any()))
                .willThrow(new BusinessException("验证失败"))
                .willReturn(new PriceVerificationResponse());

        // when
        Optional<CouponItem> result = orderService.tryVerifyCoupon(List.of(couponA, couponB), 61429);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(200);
    }

    @Test
    void should_return_empty_when_all_coupons_fail() {
        // given
        CouponItem couponA = createCoupon(100, 2, Map.of(), Map.of());
        CouponItem couponB = createCoupon(200, 2, Map.of(), Map.of());
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);
        given(apiClient.verifyPrice(any())).willThrow(new BusinessException("验证失败"));

        // when
        Optional<CouponItem> result = orderService.tryVerifyCoupon(List.of(couponA, couponB), 61429);

        // then
        assertThat(result).isEmpty();
    }

    // === placeOrder (3.2.7) ===

    @Test
    void should_create_order_with_coupon() {
        // given
        CouponItem coupon = createCoupon(8317178, 2, Map.of(), Map.of());
        CreateOrderResponse response = new CreateOrderResponse();
        response.setWxOrderId(572468);
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);
        given(apiClient.createOrder(any())).willReturn(response);

        // when
        CreateOrderResponse result = orderService.placeOrder(61429, Optional.of(coupon));

        // then
        assertThat(result.getWxOrderId()).isEqualTo(572468);
    }

    @Test
    void should_create_order_without_coupon() {
        // given
        CreateOrderResponse response = new CreateOrderResponse();
        response.setWxOrderId(572469);
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);
        given(apiClient.createOrder(any())).willReturn(response);

        // when
        CreateOrderResponse result = orderService.placeOrder(61429, Optional.empty());

        // then
        assertThat(result.getWxOrderId()).isEqualTo(572469);
    }

    // === tryCreateOrder — departure time check ===

    @Test
    void should_return_false_when_departure_time_has_passed() {
        // given: 出发时间 2026/3/25 07:40，当前时间已超过
        ScheduleItem schedule = createSchedule(61429, "2026/3/25", "07:40", 1);
        LocalDateTime pastDeparture = LocalDateTime.of(2026, 3, 25, 7, 40);

        // when
        boolean result = orderService.tryCreateOrder(schedule,
                pastDeparture.plusMinutes(1)); // 当前时间 07:41，已过出发时间

        // then
        assertThat(result).isFalse();
        verify(apiClient, never()).getRouteStops(anyInt(), anyList());
    }

    @Test
    void should_proceed_when_departure_time_not_passed() {
        // given
        ScheduleItem schedule = createSchedule(61429, "2026/3/25", "07:40", 1);
        LocalDateTime beforeDeparture = LocalDateTime.of(2026, 3, 25, 7, 39);
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);

        CouponItem coupon = createCoupon(8317178, 2, Map.of("61429", true), Map.of("61429", "待使用"));
        given(apiClient.getCoupons(275, List.of(61429), 24)).willReturn(List.of(coupon));
        given(apiClient.verifyPrice(any())).willReturn(new PriceVerificationResponse());

        CreateOrderResponse orderResponse = new CreateOrderResponse();
        orderResponse.setWxOrderId(572468);
        given(apiClient.createOrder(any())).willReturn(orderResponse);

        // when
        boolean result = orderService.tryCreateOrder(schedule, beforeDeparture);

        // then
        assertThat(result).isTrue();
    }

    // === tryCreateOrder (3.2.9) ===

    @Test
    void should_return_true_when_order_created_successfully() {
        // given
        ScheduleItem schedule = createSchedule(61429, "2026/3/25", "07:40", 1);
        LocalDateTime beforeDeparture = LocalDateTime.of(2026, 3, 25, 7, 0);
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);

        CouponItem coupon = createCoupon(8317178, 2, Map.of("61429", true), Map.of("61429", "待使用"));
        given(apiClient.getCoupons(275, List.of(61429), 24)).willReturn(List.of(coupon));
        given(apiClient.verifyPrice(any())).willReturn(new PriceVerificationResponse());

        CreateOrderResponse orderResponse = new CreateOrderResponse();
        orderResponse.setWxOrderId(572468);
        given(apiClient.createOrder(any())).willReturn(orderResponse);

        // when
        boolean result = orderService.tryCreateOrder(schedule, beforeDeparture);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void should_return_true_when_all_coupons_fail_and_order_without_coupon() {
        // given
        ScheduleItem schedule = createSchedule(61429, "2026/3/25", "07:40", 1);
        LocalDateTime beforeDeparture = LocalDateTime.of(2026, 3, 25, 7, 0);
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);

        CouponItem coupon = createCoupon(100, 2, Map.of("61429", true), Map.of("61429", "待使用"));
        given(apiClient.getCoupons(275, List.of(61429), 24)).willReturn(List.of(coupon));
        given(apiClient.verifyPrice(any())).willThrow(new BusinessException("验证失败"));

        CreateOrderResponse orderResponse = new CreateOrderResponse();
        orderResponse.setWxOrderId(572469);
        given(apiClient.createOrder(any())).willReturn(orderResponse);

        // when
        boolean result = orderService.tryCreateOrder(schedule, beforeDeparture);

        // then
        assertThat(result).isTrue();
    }

    // === findPaidOrderDates ===

    @Test
    void should_return_paid_dates_matching_route_and_target_dates() {
        // given
        RouteStopsResponse stopsResponse = new RouteStopsResponse();
        stopsResponse.setRouteName("17号线-明珠线-上班");
        given(apiClient.getRouteStops(275, List.of(61429))).willReturn(stopsResponse);

        given(apiClient.getOrders("待支付", 1, 10)).willReturn(List.of());

        OrderItem paidOrder = createOrder(572468, "17号线-明珠线-上班", "2026-03-25 07:40:00", "支付成功");
        given(apiClient.getOrders("已支付", 1, 10)).willReturn(List.of(paidOrder));

        Set<LocalDate> targetDates = Set.of(
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 26));

        // when
        Set<LocalDate> result = orderService.findPaidOrderDates(275, 61429, targetDates);

        // then
        assertThat(result).containsExactly(LocalDate.of(2026, 3, 25));
    }

    @Test
    void should_return_empty_when_paid_orders_do_not_match_route() {
        // given
        RouteStopsResponse stopsResponse = new RouteStopsResponse();
        stopsResponse.setRouteName("17号线-明珠线-上班");
        given(apiClient.getRouteStops(275, List.of(61429))).willReturn(stopsResponse);

        given(apiClient.getOrders("待支付", 1, 10)).willReturn(List.of());

        OrderItem paidOrder = createOrder(572468, "4号线-上冲/香洲-下班", "2026-03-25 18:25:00", "支付成功");
        given(apiClient.getOrders("已支付", 1, 10)).willReturn(List.of(paidOrder));

        Set<LocalDate> targetDates = Set.of(LocalDate.of(2026, 3, 25));

        // when
        Set<LocalDate> result = orderService.findPaidOrderDates(275, 61429, targetDates);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void should_return_empty_when_no_paid_orders() {
        // given
        RouteStopsResponse stopsResponse = new RouteStopsResponse();
        stopsResponse.setRouteName("17号线-明珠线-上班");
        given(apiClient.getRouteStops(275, List.of(61429))).willReturn(stopsResponse);

        given(apiClient.getOrders("待支付", 1, 10)).willReturn(List.of());
        given(apiClient.getOrders("已支付", 1, 10)).willReturn(List.of());

        Set<LocalDate> targetDates = Set.of(LocalDate.of(2026, 3, 25));

        // when
        Set<LocalDate> result = orderService.findPaidOrderDates(275, 61429, targetDates);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void should_list_all_dates_in_unpaid_order_warning_for_multi_schedule_orders() {
        // given: 一个合并的待支付订单包含 3 个日期
        RouteStopsResponse stopsResponse = new RouteStopsResponse();
        stopsResponse.setRouteName("17号线-明珠线-上班");
        given(apiClient.getRouteStops(275, List.of(61429))).willReturn(stopsResponse);

        OrderItem unpaidOrder = new OrderItem();
        unpaidOrder.setId(572468);
        unpaidOrder.setRouteName("17号线-明珠线-上班");
        unpaidOrder.setTradeState("待支付");
        OrderDescription desc = new OrderDescription();
        desc.setStartStop("长沙圩①");
        desc.setEndStop("科创中心西门");
        desc.setDate(List.of(
                "2026-03-25 07:40:00",
                "2026-03-26 07:40:00",
                "2026-03-27 07:40:00"));
        unpaidOrder.setDescription(desc);
        given(apiClient.getOrders("待支付", 1, 10)).willReturn(List.of(unpaidOrder));

        Set<LocalDate> targetDates = Set.of(LocalDate.of(2026, 3, 25));

        // when & then: warning 文案 MUST 包含全部 3 个乘车日期
        assertThatThrownBy(() -> orderService.findPaidOrderDates(275, 61429, targetDates))
                .isInstanceOf(UnpaidOrderException.class)
                .satisfies(e -> {
                    String msg = e.getMessage();
                    assertThat(msg).contains("2026-03-25 07:40:00");
                    assertThat(msg).contains("2026-03-26 07:40:00");
                    assertThat(msg).contains("2026-03-27 07:40:00");
                });
    }

    @Test
    void should_throw_when_unpaid_order_exists() {
        // given
        RouteStopsResponse stopsResponse = new RouteStopsResponse();
        stopsResponse.setRouteName("17号线-明珠线-上班");
        given(apiClient.getRouteStops(275, List.of(61429))).willReturn(stopsResponse);

        OrderItem unpaidOrder = createOrder(572468, "17号线-明珠线-上班", "2026-03-25 07:40:00", "待支付");
        given(apiClient.getOrders("待支付", 1, 10)).willReturn(List.of(unpaidOrder));

        Set<LocalDate> targetDates = Set.of(LocalDate.of(2026, 3, 25));

        // when & then
        assertThatThrownBy(() -> orderService.findPaidOrderDates(275, 61429, targetDates))
                .isInstanceOf(UnpaidOrderException.class)
                .hasMessageContaining("待支付");

        verify(apiClient, never()).getOrders(eq("已支付"), anyInt(), anyInt());
    }

    @Test
    void should_check_unpaid_before_paid_in_find_paid_order_dates() {
        // given
        RouteStopsResponse stopsResponse = new RouteStopsResponse();
        stopsResponse.setRouteName("17号线-明珠线-上班");
        given(apiClient.getRouteStops(275, List.of(61429))).willReturn(stopsResponse);

        given(apiClient.getOrders("待支付", 1, 10)).willReturn(List.of());
        given(apiClient.getOrders("已支付", 1, 10)).willReturn(List.of());

        Set<LocalDate> targetDates = Set.of(LocalDate.of(2026, 3, 25));

        // when
        orderService.findPaidOrderDates(275, 61429, targetDates);

        // then
        var inOrder = inOrder(apiClient);
        inOrder.verify(apiClient).getOrders("待支付", 1, 10);
        inOrder.verify(apiClient).getOrders("已支付", 1, 10);
    }

    // === helpers ===

    private OrderItem createOrder(int id, String routeName, String dateTime, String tradeState) {
        OrderItem order = new OrderItem();
        order.setId(id);
        order.setRouteName(routeName);
        order.setTradeState(tradeState);
        OrderDescription desc = new OrderDescription();
        desc.setDate(List.of(dateTime));
        desc.setStartStop("长沙圩①");
        desc.setEndStop("科创中心西门");
        order.setDescription(desc);
        return order;
    }

    private CouponItem createCoupon(int id, int categoryId, Map<String, Boolean> isUse, Map<String, String> status) {
        CouponItem coupon = new CouponItem();
        coupon.setId(id);
        coupon.setCouponCategoryId(categoryId);
        coupon.setIsUse(isUse);
        coupon.setStatus(status);
        return coupon;
    }

    private ScheduleItem createSchedule(int id, String date, int number) {
        return createSchedule(id, date, "07:40", number);
    }

    private ScheduleItem createSchedule(int id, String date, String time, int number) {
        ScheduleItem item = new ScheduleItem();
        item.setId(id);
        item.setDate(date);
        item.setTime(time);
        item.setNumber(number);
        return item;
    }

    // === Phase 2 Foundational: tryCreateMultiScheduleOrder input validation ===

    @Test
    void should_throw_business_exception_when_multi_schedule_list_is_empty() {
        // given
        List<ScheduleItem> empty = List.of();

        // when & then
        assertThatThrownBy(() -> orderService.tryCreateMultiScheduleOrder(empty))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("schedule");

        verify(apiClient, never()).createOrder(any());
    }

    @Test
    void should_throw_business_exception_when_multi_schedule_list_is_null() {
        // when & then
        assertThatThrownBy(() -> orderService.tryCreateMultiScheduleOrder(null))
                .isInstanceOf(BusinessException.class);

        verify(apiClient, never()).createOrder(any());
    }

    @Test
    void should_create_single_order_for_two_schedules_with_no_coupons() {
        // given
        ScheduleItem s1 = createSchedule(61429, "2026/3/25", "07:40", 1);
        ScheduleItem s2 = createSchedule(61512, "2026/3/26", "07:40", 1);
        LocalDateTime beforeBoth = LocalDateTime.of(2026, 3, 25, 6, 0);
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);

        CreateOrderResponse response = new CreateOrderResponse();
        response.setWxOrderId(999001);
        ArgumentCaptor<CreateOrderRequest> captor = ArgumentCaptor.forClass(CreateOrderRequest.class);
        given(apiClient.createOrder(captor.capture())).willReturn(response);

        // when
        boolean result = orderService.tryCreateMultiScheduleOrder(List.of(s1, s2), beforeBoth);

        // then
        assertThat(result).isTrue();
        assertThat(orderService.getLastCreatedOrderId()).isEqualTo(999001);
        CreateOrderRequest sent = captor.getValue();
        assertThat(sent.getRouteId()).isEqualTo(275);
        assertThat(sent.getBoardingPointId()).isEqualTo(24);
        assertThat(sent.getAlightingPointId()).isEqualTo(400);
        assertThat(sent.getScheduleIds()).containsExactly(61429, 61512);
        assertThat(sent.getCouponIds()).isEmpty();
        verify(apiClient, times(1)).createOrder(any());
    }

    @Test
    void should_deduplicate_schedules_before_submitting_multi_order() {
        // given
        ScheduleItem s1 = createSchedule(61429, "2026/3/25", "07:40", 1);
        ScheduleItem s1Dup = createSchedule(61429, "2026/3/25", "07:40", 1);
        ScheduleItem s2 = createSchedule(61512, "2026/3/26", "07:40", 1);
        LocalDateTime beforeBoth = LocalDateTime.of(2026, 3, 25, 6, 0);
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);

        CreateOrderResponse response = new CreateOrderResponse();
        response.setWxOrderId(999002);
        ArgumentCaptor<CreateOrderRequest> captor = ArgumentCaptor.forClass(CreateOrderRequest.class);
        given(apiClient.createOrder(captor.capture())).willReturn(response);

        // when
        boolean result = orderService.tryCreateMultiScheduleOrder(
                List.of(s1, s1Dup, s2), beforeBoth);

        // then
        assertThat(result).isTrue();
        assertThat(captor.getValue().getScheduleIds()).containsExactly(61429, 61512);
        verify(apiClient, times(1)).createOrder(any());
    }

    @Test
    void should_skip_departed_schedule_but_keep_others_in_multi_order() {
        // given: s1 已发车，s2 未发车
        ScheduleItem s1Departed = createSchedule(61429, "2026/3/25", "07:40", 1);
        ScheduleItem s2 = createSchedule(61512, "2026/3/26", "07:40", 1);
        LocalDateTime afterS1BeforeS2 = LocalDateTime.of(2026, 3, 25, 8, 0);
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);

        CreateOrderResponse response = new CreateOrderResponse();
        response.setWxOrderId(999003);
        ArgumentCaptor<CreateOrderRequest> captor = ArgumentCaptor.forClass(CreateOrderRequest.class);
        given(apiClient.createOrder(captor.capture())).willReturn(response);

        // when
        boolean result = orderService.tryCreateMultiScheduleOrder(
                List.of(s1Departed, s2), afterS1BeforeS2);

        // then
        assertThat(result).isTrue();
        assertThat(captor.getValue().getScheduleIds()).containsExactly(61512);
    }

    @Test
    void should_handle_n_equals_1_via_multi_schedule_entry_without_coupons() {
        // given: N=1 path through the new multi-schedule entry, no usable coupons available
        // Full "N=1 with coupons" equivalence is verified in Phase 4 (US2) after delegation lands.
        ScheduleItem schedule = createSchedule(61429, "2026/3/25", "07:40", 1);
        LocalDateTime beforeDeparture = LocalDateTime.of(2026, 3, 25, 7, 0);
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);

        CreateOrderResponse response = new CreateOrderResponse();
        response.setWxOrderId(572468);
        ArgumentCaptor<CreateOrderRequest> captor = ArgumentCaptor.forClass(CreateOrderRequest.class);
        given(apiClient.createOrder(captor.capture())).willReturn(response);

        // when
        boolean result = orderService.tryCreateMultiScheduleOrder(List.of(schedule), beforeDeparture);

        // then
        assertThat(result).isTrue();
        CreateOrderRequest sent = captor.getValue();
        assertThat(sent.getScheduleIds()).containsExactly(61429);
        assertThat(sent.getCouponIds()).isEmpty();
        assertThat(sent.getRouteId()).isEqualTo(275);
        assertThat(sent.getBoardingPointId()).isEqualTo(24);
        assertThat(sent.getAlightingPointId()).isEqualTo(400);
    }

    // === Phase 4 US2: Earliest-date-first coupon allocation ===

    @Test
    void should_assign_distinct_coupons_to_each_schedule_when_two_independent_coupons_available() {
        // given: 两个未发车车次，每张券分别只对一个 schedule 可用
        ScheduleItem s1 = createSchedule(61429, "2026/3/25", "07:40", 1);
        ScheduleItem s2 = createSchedule(61512, "2026/3/26", "07:40", 1);
        LocalDateTime before = LocalDateTime.of(2026, 3, 25, 6, 0);
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);

        CouponItem c1 = createCoupon(1001, 2,
                Map.of("61429", true, "61512", false),
                Map.of("61429", "待使用", "61512", "不可使用"));
        CouponItem c2 = createCoupon(1002, 2,
                Map.of("61429", false, "61512", true),
                Map.of("61429", "不可使用", "61512", "待使用"));
        given(apiClient.getCoupons(275, List.of(61429, 61512), 24))
                .willReturn(List.of(c1, c2));
        given(apiClient.verifyPrice(any())).willReturn(new PriceVerificationResponse());

        CreateOrderResponse response = new CreateOrderResponse();
        response.setWxOrderId(999100);
        ArgumentCaptor<CreateOrderRequest> captor = ArgumentCaptor.forClass(CreateOrderRequest.class);
        given(apiClient.createOrder(captor.capture())).willReturn(response);

        // when
        boolean result = orderService.tryCreateMultiScheduleOrder(List.of(s1, s2), before);

        // then
        assertThat(result).isTrue();
        Map<String, Map<String, Integer>> couponIds = captor.getValue().getCouponIds();
        assertThat(couponIds).containsEntry("61429", Map.of("2", 1001));
        assertThat(couponIds).containsEntry("61512", Map.of("2", 1002));
        assertThat(couponIds).hasSize(2);
    }

    @Test
    void should_allocate_shared_coupon_to_earliest_schedule_when_only_one_shared_coupon_available() {
        // given: 单张优惠券对两个车次均可用；按 earliest-date-first 应分给较早的车次
        ScheduleItem early = createSchedule(61429, "2026/3/25", "07:40", 1);
        ScheduleItem later = createSchedule(61512, "2026/3/26", "07:40", 1);
        LocalDateTime before = LocalDateTime.of(2026, 3, 25, 6, 0);
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);

        CouponItem shared = createCoupon(7777, 2,
                Map.of("61429", true, "61512", true),
                Map.of("61429", "待使用", "61512", "待使用"));
        given(apiClient.getCoupons(275, List.of(61429, 61512), 24))
                .willReturn(List.of(shared));
        given(apiClient.verifyPrice(any())).willReturn(new PriceVerificationResponse());

        CreateOrderResponse response = new CreateOrderResponse();
        response.setWxOrderId(999101);
        ArgumentCaptor<CreateOrderRequest> captor = ArgumentCaptor.forClass(CreateOrderRequest.class);
        given(apiClient.createOrder(captor.capture())).willReturn(response);

        // when
        boolean result = orderService.tryCreateMultiScheduleOrder(List.of(later, early), before);  // 故意乱序传入

        // then
        assertThat(result).isTrue();
        Map<String, Map<String, Integer>> couponIds = captor.getValue().getCouponIds();
        assertThat(couponIds).containsOnlyKeys("61429");  // 仅较早车次获得券
        assertThat(couponIds).containsEntry("61429", Map.of("2", 7777));
    }

    @Test
    void should_leave_later_schedule_without_coupon_when_earliest_consumes_its_only_option() {
        // given: 较早车次有 3 张可用券（含 X），较晚车次仅有 X；earliest 消费 X 后较晚车次无券
        ScheduleItem early = createSchedule(61429, "2026/3/25", "07:40", 1);
        ScheduleItem later = createSchedule(61512, "2026/3/26", "07:40", 1);
        LocalDateTime before = LocalDateTime.of(2026, 3, 25, 6, 0);
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);

        // X 对两个车次都可用；Y、Z 仅对 early 可用
        CouponItem couponX = createCoupon(5001, 2,
                Map.of("61429", true, "61512", true),
                Map.of("61429", "待使用", "61512", "待使用"));
        CouponItem couponY = createCoupon(5002, 2,
                Map.of("61429", true, "61512", false),
                Map.of("61429", "待使用", "61512", "不可使用"));
        CouponItem couponZ = createCoupon(5003, 2,
                Map.of("61429", true, "61512", false),
                Map.of("61429", "待使用", "61512", "不可使用"));
        // X 排在第一位 -> early 会先消费它
        given(apiClient.getCoupons(275, List.of(61429, 61512), 24))
                .willReturn(List.of(couponX, couponY, couponZ));
        given(apiClient.verifyPrice(any())).willReturn(new PriceVerificationResponse());

        CreateOrderResponse response = new CreateOrderResponse();
        response.setWxOrderId(999102);
        ArgumentCaptor<CreateOrderRequest> captor = ArgumentCaptor.forClass(CreateOrderRequest.class);
        given(apiClient.createOrder(captor.capture())).willReturn(response);

        // when
        boolean result = orderService.tryCreateMultiScheduleOrder(List.of(early, later), before);

        // then
        assertThat(result).isTrue();
        Map<String, Map<String, Integer>> couponIds = captor.getValue().getCouponIds();
        assertThat(couponIds).containsOnlyKeys("61429");
        assertThat(couponIds.get("61429")).containsEntry("2", 5001);  // X 被 early 消费
    }

    @Test
    void should_call_get_coupons_once_for_all_schedules() {
        // given
        ScheduleItem s1 = createSchedule(61429, "2026/3/25", "07:40", 1);
        ScheduleItem s2 = createSchedule(61512, "2026/3/26", "07:40", 1);
        ScheduleItem s3 = createSchedule(61777, "2026/3/27", "07:40", 1);
        LocalDateTime before = LocalDateTime.of(2026, 3, 25, 6, 0);
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);

        given(apiClient.getCoupons(eq(275), anyList(), eq(24))).willReturn(List.of());
        CreateOrderResponse response = new CreateOrderResponse();
        response.setWxOrderId(999103);
        given(apiClient.createOrder(any())).willReturn(response);

        // when
        orderService.tryCreateMultiScheduleOrder(List.of(s1, s2, s3), before);

        // then: 一次 getCoupons 调用承载所有 schedule_ids
        verify(apiClient, times(1)).getCoupons(275, List.of(61429, 61512, 61777), 24);
    }

    // === Phase 6 Polish: audit logging (FR-011) ===

    @Test
    void should_log_schedule_ids_and_coupon_assignment_when_attempting_multi_schedule_order() {
        // given
        Logger logger = (Logger) LoggerFactory.getLogger(OrderService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        try {
            ScheduleItem s1 = createSchedule(61429, "2026/3/25", "07:40", 1);
            ScheduleItem s2 = createSchedule(61512, "2026/3/26", "07:40", 1);
            LocalDateTime before = LocalDateTime.of(2026, 3, 25, 6, 0);
            given(properties.getRouteId()).willReturn(275);
            given(properties.getBoardingPointId()).willReturn(24);
            given(properties.getAlightingPointId()).willReturn(400);

            CouponItem c1 = createCoupon(7001, 2,
                    Map.of("61429", true, "61512", false),
                    Map.of("61429", "待使用", "61512", "不可使用"));
            given(apiClient.getCoupons(275, List.of(61429, 61512), 24))
                    .willReturn(List.of(c1));
            given(apiClient.verifyPrice(any())).willReturn(new PriceVerificationResponse());

            CreateOrderResponse response = new CreateOrderResponse();
            response.setWxOrderId(999300);
            given(apiClient.createOrder(any())).willReturn(response);

            // when
            orderService.tryCreateMultiScheduleOrder(List.of(s1, s2), before);

            // then: 至少有一条 INFO 日志同时包含 schedule_ids 与 coupon assignment 摘要
            assertThat(appender.list)
                    .filteredOn(e -> e.getLevel() == Level.INFO)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .anyMatch(msg -> msg.contains("61429")
                            && msg.contains("61512")
                            && msg.contains("7001")
                            && msg.contains("999300"));
        } finally {
            logger.detachAppender(appender);
        }
    }

    // === DEBUG logs for createOrder request/response ===

    @Test
    void should_log_create_order_request_and_response_at_debug_level_in_multi_schedule_flow() {
        // given
        Logger logger = (Logger) LoggerFactory.getLogger(OrderService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        Level original = logger.getLevel();
        logger.setLevel(Level.DEBUG);

        try {
            ScheduleItem s1 = createSchedule(61429, "2026/3/25", "07:40", 1);
            ScheduleItem s2 = createSchedule(61512, "2026/3/26", "07:40", 1);
            LocalDateTime before = LocalDateTime.of(2026, 3, 25, 6, 0);
            given(properties.getRouteId()).willReturn(275);
            given(properties.getBoardingPointId()).willReturn(24);
            given(properties.getAlightingPointId()).willReturn(400);

            CouponItem c1 = createCoupon(9001, 2,
                    Map.of("61429", true, "61512", false),
                    Map.of("61429", "待使用", "61512", "不可使用"));
            given(apiClient.getCoupons(275, List.of(61429, 61512), 24))
                    .willReturn(List.of(c1));
            given(apiClient.verifyPrice(any())).willReturn(new PriceVerificationResponse());

            CreateOrderResponse response = new CreateOrderResponse();
            response.setWxOrderId(999600);
            given(apiClient.createOrder(any())).willReturn(response);

            // when
            orderService.tryCreateMultiScheduleOrder(List.of(s1, s2), before);

            // then
            List<String> debugMsgs = appender.list.stream()
                    .filter(e -> e.getLevel() == Level.DEBUG)
                    .map(ILoggingEvent::getFormattedMessage)
                    .toList();

            assertThat(debugMsgs)
                    .as("DEBUG 日志必须包含 createOrder 请求报文，含 scheduleIds 与 couponIds")
                    .anyMatch(m -> m.contains("请求报文")
                            && m.contains("61429")
                            && m.contains("61512")
                            && m.contains("9001"));
            assertThat(debugMsgs)
                    .as("DEBUG 日志必须包含 createOrder 响应报文，含 wx_order_id")
                    .anyMatch(m -> m.contains("响应报文")
                            && m.contains("999600"));
        } finally {
            logger.setLevel(original);
            logger.detachAppender(appender);
        }
    }

    // === Final batch verifyPrice before createOrder (mirror tryCreateOrder's "verify-then-create" pattern) ===

    @Test
    void should_call_batch_verify_price_with_all_schedules_and_coupons_before_create_order() {
        // given
        ScheduleItem s1 = createSchedule(61429, "2026/3/25", "07:40", 1);
        ScheduleItem s2 = createSchedule(61512, "2026/3/26", "07:40", 1);
        LocalDateTime before = LocalDateTime.of(2026, 3, 25, 6, 0);
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);

        CouponItem c1 = createCoupon(8001, 2,
                Map.of("61429", true, "61512", false),
                Map.of("61429", "待使用", "61512", "不可使用"));
        CouponItem c2 = createCoupon(8002, 2,
                Map.of("61429", false, "61512", true),
                Map.of("61429", "不可使用", "61512", "待使用"));
        given(apiClient.getCoupons(275, List.of(61429, 61512), 24))
                .willReturn(List.of(c1, c2));
        given(apiClient.verifyPrice(any())).willReturn(new PriceVerificationResponse());

        CreateOrderResponse response = new CreateOrderResponse();
        response.setWxOrderId(999400);
        given(apiClient.createOrder(any())).willReturn(response);

        ArgumentCaptor<PriceVerificationRequest> verifyCaptor =
                ArgumentCaptor.forClass(PriceVerificationRequest.class);

        // when
        boolean result = orderService.tryCreateMultiScheduleOrder(List.of(s1, s2), before);

        // then
        assertThat(result).isTrue();
        // Capture every verifyPrice invocation (2 per-coupon during allocation + 1 final batch)
        verify(apiClient, atLeast(3)).verifyPrice(verifyCaptor.capture());

        // 最后一次 verifyPrice 必须是承载全部 (schedule, coupon) 对的批量请求
        List<PriceVerificationRequest> allVerifyCalls = verifyCaptor.getAllValues();
        PriceVerificationRequest finalReq = allVerifyCalls.get(allVerifyCalls.size() - 1);
        assertThat(finalReq.getScheduleIds()).containsExactly(61429, 61512);
        assertThat(finalReq.getCouponIds()).containsEntry("61429", Map.of("2", 8001));
        assertThat(finalReq.getCouponIds()).containsEntry("61512", Map.of("2", 8002));
        assertThat(finalReq.getRouteId()).isEqualTo(275);
        assertThat(finalReq.getBoardingPointId()).isEqualTo(24);
        assertThat(finalReq.getAlightingPointId()).isEqualTo(400);

        // 最终批量 verifyPrice 必须在 createOrder 之前发生
        ArgumentCaptor<PriceVerificationRequest> batchCaptor =
                ArgumentCaptor.forClass(PriceVerificationRequest.class);
        var inOrder = inOrder(apiClient);
        inOrder.verify(apiClient).verifyPrice(argThat(req ->
                req != null && req.getScheduleIds() != null && req.getScheduleIds().size() > 1));
        inOrder.verify(apiClient).createOrder(any());
    }

    @Test
    void should_skip_create_order_when_final_batch_verification_fails() {
        // given: 单 schedule、单 coupon 验证成功；但最终批量验证失败 → 不创建订单
        ScheduleItem s1 = createSchedule(61429, "2026/3/25", "07:40", 1);
        ScheduleItem s2 = createSchedule(61512, "2026/3/26", "07:40", 1);
        LocalDateTime before = LocalDateTime.of(2026, 3, 25, 6, 0);
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);

        CouponItem c1 = createCoupon(8101, 2,
                Map.of("61429", true, "61512", false),
                Map.of("61429", "待使用", "61512", "不可使用"));
        CouponItem c2 = createCoupon(8102, 2,
                Map.of("61429", false, "61512", true),
                Map.of("61429", "不可使用", "61512", "待使用"));
        given(apiClient.getCoupons(275, List.of(61429, 61512), 24))
                .willReturn(List.of(c1, c2));

        // 单 schedule_id 的请求（per-coupon 验证）→ 成功；多 schedule_id 的最终请求 → 失败
        given(apiClient.verifyPrice(argThat(req ->
                req != null && req.getScheduleIds() != null && req.getScheduleIds().size() == 1)))
                .willReturn(new PriceVerificationResponse());
        given(apiClient.verifyPrice(argThat(req ->
                req != null && req.getScheduleIds() != null && req.getScheduleIds().size() > 1)))
                .willThrow(new BusinessException("最终批量验证失败"));

        // when & then
        assertThatThrownBy(() ->
                orderService.tryCreateMultiScheduleOrder(List.of(s1, s2), before))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("最终批量验证");
        verify(apiClient, never()).createOrder(any());
    }

    @Test
    void should_call_final_batch_verify_even_when_assignment_is_empty() {
        // given: 两个 schedule 均无可用券 → 仍然要做最终批量 verifyPrice（用户要求"任何情况下都应该 verify-price"）
        ScheduleItem s1 = createSchedule(61429, "2026/3/25", "07:40", 1);
        ScheduleItem s2 = createSchedule(61512, "2026/3/26", "07:40", 1);
        LocalDateTime before = LocalDateTime.of(2026, 3, 25, 6, 0);
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);
        given(apiClient.getCoupons(275, List.of(61429, 61512), 24)).willReturn(List.of());
        given(apiClient.verifyPrice(any())).willReturn(new PriceVerificationResponse());

        CreateOrderResponse response = new CreateOrderResponse();
        response.setWxOrderId(999500);
        given(apiClient.createOrder(any())).willReturn(response);

        ArgumentCaptor<PriceVerificationRequest> verifyCaptor =
                ArgumentCaptor.forClass(PriceVerificationRequest.class);

        // when
        boolean result = orderService.tryCreateMultiScheduleOrder(List.of(s1, s2), before);

        // then
        assertThat(result).isTrue();
        verify(apiClient, times(1)).verifyPrice(verifyCaptor.capture());
        verify(apiClient, times(1)).createOrder(any());

        // 该唯一一次 verifyPrice 必须承载全部 scheduleIds，coupon_ids 为空 map（非 null）
        PriceVerificationRequest req = verifyCaptor.getValue();
        assertThat(req.getScheduleIds()).containsExactly(61429, 61512);
        assertThat(req.getCouponIds()).isNotNull();
        assertThat(req.getCouponIds()).isEmpty();
        assertThat(req.getRouteId()).isEqualTo(275);
        assertThat(req.getBoardingPointId()).isEqualTo(24);
        assertThat(req.getAlightingPointId()).isEqualTo(400);

        // verifyPrice 必须发生在 createOrder 之前
        var inOrder = inOrder(apiClient);
        inOrder.verify(apiClient).verifyPrice(any());
        inOrder.verify(apiClient).createOrder(any());
    }

    // === Phase 5 US3: graceful degradation when some/no/failed coupons ===

    @Test
    void should_create_order_when_only_one_schedule_has_usable_coupon() {
        // given: schedule_1 有可用券 C1，schedule_2 完全无可用券
        ScheduleItem s1 = createSchedule(61429, "2026/3/25", "07:40", 1);
        ScheduleItem s2 = createSchedule(61512, "2026/3/26", "07:40", 1);
        LocalDateTime before = LocalDateTime.of(2026, 3, 25, 6, 0);
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);

        CouponItem c1 = createCoupon(3001, 2,
                Map.of("61429", true, "61512", false),
                Map.of("61429", "待使用", "61512", "不可使用"));
        given(apiClient.getCoupons(275, List.of(61429, 61512), 24))
                .willReturn(List.of(c1));
        given(apiClient.verifyPrice(any())).willReturn(new PriceVerificationResponse());

        CreateOrderResponse response = new CreateOrderResponse();
        response.setWxOrderId(999200);
        ArgumentCaptor<CreateOrderRequest> captor = ArgumentCaptor.forClass(CreateOrderRequest.class);
        given(apiClient.createOrder(captor.capture())).willReturn(response);

        // when
        boolean result = orderService.tryCreateMultiScheduleOrder(List.of(s1, s2), before);

        // then: 订单创建成功，coupon_ids 仅含 s1
        assertThat(result).isTrue();
        Map<String, Map<String, Integer>> couponIds = captor.getValue().getCouponIds();
        assertThat(couponIds).containsOnlyKeys("61429");
        assertThat(couponIds).containsEntry("61429", Map.of("2", 3001));
    }

    @Test
    void should_create_order_with_empty_coupon_ids_when_no_schedules_have_coupons() {
        // given: 两个车次均无可用券
        ScheduleItem s1 = createSchedule(61429, "2026/3/25", "07:40", 1);
        ScheduleItem s2 = createSchedule(61512, "2026/3/26", "07:40", 1);
        LocalDateTime before = LocalDateTime.of(2026, 3, 25, 6, 0);
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);

        given(apiClient.getCoupons(275, List.of(61429, 61512), 24)).willReturn(List.of());

        CreateOrderResponse response = new CreateOrderResponse();
        response.setWxOrderId(999201);
        ArgumentCaptor<CreateOrderRequest> captor = ArgumentCaptor.forClass(CreateOrderRequest.class);
        given(apiClient.createOrder(captor.capture())).willReturn(response);

        // when
        boolean result = orderService.tryCreateMultiScheduleOrder(List.of(s1, s2), before);

        // then: 订单仍创建，coupon_ids 是空 Map（非 null）
        assertThat(result).isTrue();
        Map<String, Map<String, Integer>> couponIds = captor.getValue().getCouponIds();
        assertThat(couponIds).isNotNull();
        assertThat(couponIds).isEmpty();
    }

    @Test
    void should_create_order_when_all_coupons_for_one_schedule_fail_verification_but_another_succeeds() {
        // given: s1 有 2 张候选券但都验证失败；s2 有 C2 验证成功
        ScheduleItem s1 = createSchedule(61429, "2026/3/25", "07:40", 1);
        ScheduleItem s2 = createSchedule(61512, "2026/3/26", "07:40", 1);
        LocalDateTime before = LocalDateTime.of(2026, 3, 25, 6, 0);
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);

        CouponItem badForS1A = createCoupon(4001, 2,
                Map.of("61429", true, "61512", false),
                Map.of("61429", "待使用", "61512", "不可使用"));
        CouponItem badForS1B = createCoupon(4002, 2,
                Map.of("61429", true, "61512", false),
                Map.of("61429", "待使用", "61512", "不可使用"));
        CouponItem goodForS2 = createCoupon(4003, 2,
                Map.of("61429", false, "61512", true),
                Map.of("61429", "不可使用", "61512", "待使用"));
        given(apiClient.getCoupons(275, List.of(61429, 61512), 24))
                .willReturn(List.of(badForS1A, badForS1B, goodForS2));

        // 4001、4002 验证失败；4003 验证成功
        given(apiClient.verifyPrice(argThat(req -> {
            if (req == null || req.getCouponIds() == null) return false;
            return req.getCouponIds().values().stream()
                    .flatMap(m -> m.values().stream())
                    .anyMatch(id -> id == 4001 || id == 4002);
        }))).willThrow(new BusinessException("验证失败"));
        given(apiClient.verifyPrice(argThat(req -> {
            if (req == null || req.getCouponIds() == null) return false;
            return req.getCouponIds().values().stream()
                    .flatMap(m -> m.values().stream())
                    .anyMatch(id -> id == 4003);
        }))).willReturn(new PriceVerificationResponse());

        CreateOrderResponse response = new CreateOrderResponse();
        response.setWxOrderId(999202);
        ArgumentCaptor<CreateOrderRequest> captor = ArgumentCaptor.forClass(CreateOrderRequest.class);
        given(apiClient.createOrder(captor.capture())).willReturn(response);

        // when
        boolean result = orderService.tryCreateMultiScheduleOrder(List.of(s1, s2), before);

        // then: 订单成功创建，coupon_ids 仅含 s2
        assertThat(result).isTrue();
        Map<String, Map<String, Integer>> couponIds = captor.getValue().getCouponIds();
        assertThat(couponIds).containsOnlyKeys("61512");
        assertThat(couponIds).containsEntry("61512", Map.of("2", 4003));
    }

    @Test
    void should_return_false_when_all_schedules_already_departed_in_multi_order() {
        // given
        ScheduleItem departed1 = createSchedule(61429, "2026/3/25", "07:40", 1);
        ScheduleItem departed2 = createSchedule(61512, "2026/3/26", "07:40", 1);
        LocalDateTime now = LocalDateTime.of(2026, 3, 27, 0, 0); // 已超过两个车次的出发时间

        // when
        boolean result = orderService.tryCreateMultiScheduleOrder(List.of(departed1, departed2), now);

        // then
        assertThat(result).isFalse();
        verify(apiClient, never()).createOrder(any());
        verify(apiClient, never()).getCoupons(anyInt(), anyList(), anyInt());
    }
}
