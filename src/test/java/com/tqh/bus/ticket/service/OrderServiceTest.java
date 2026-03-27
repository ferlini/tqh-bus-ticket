package com.tqh.bus.ticket.service;

import com.tqh.bus.ticket.common.BusinessException;
import com.tqh.bus.ticket.common.UnpaidOrderException;
import com.tqh.bus.ticket.config.TqhProperties;
import com.tqh.bus.ticket.integration.TqhApiClient;
import com.tqh.bus.ticket.integration.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    // === hasExistingOrder (3.2.1) ===

    @Test
    void should_throw_and_log_when_unpaid_order_exists() {
        // given
        RouteStopsResponse stopsResponse = new RouteStopsResponse();
        stopsResponse.setRouteName("17号线-明珠线-上班");
        given(apiClient.getRouteStops(275, List.of(61429))).willReturn(stopsResponse);

        OrderItem order = createOrder(572468, "17号线-明珠线-上班", "2026-03-25 07:40:00", "待支付");
        given(apiClient.getOrders("待支付", 1, 10)).willReturn(List.of(order));

        // when & then
        assertThatThrownBy(() -> orderService.hasExistingOrder(275, 61429, LocalDate.of(2026, 3, 25)))
                .isInstanceOf(UnpaidOrderException.class)
                .hasMessageContaining("待支付");

        verify(apiClient, never()).getOrders(eq("已支付"), anyInt(), anyInt());
    }

    @Test
    void should_return_true_when_paid_order_exists() {
        // given
        RouteStopsResponse stopsResponse = new RouteStopsResponse();
        stopsResponse.setRouteName("17号线-明珠线-上班");
        given(apiClient.getRouteStops(275, List.of(61429))).willReturn(stopsResponse);

        given(apiClient.getOrders("待支付", 1, 10)).willReturn(List.of());

        OrderItem order = createOrder(572468, "17号线-明珠线-上班", "2026-03-25 07:40:00", "支付成功");
        given(apiClient.getOrders("已支付", 1, 10)).willReturn(List.of(order));

        // when
        boolean result = orderService.hasExistingOrder(275, 61429, LocalDate.of(2026, 3, 25));

        // then
        assertThat(result).isTrue();
    }

    @Test
    void should_return_false_when_no_matching_order() {
        // given
        RouteStopsResponse stopsResponse = new RouteStopsResponse();
        stopsResponse.setRouteName("17号线-明珠线-上班");
        given(apiClient.getRouteStops(275, List.of(61429))).willReturn(stopsResponse);

        OrderItem order = createOrder(572468, "4号线-上冲/香洲-下班", "2026-03-25 18:25:00", "支付成功");
        given(apiClient.getOrders("待支付", 1, 10)).willReturn(List.of());
        given(apiClient.getOrders("已支付", 1, 10)).willReturn(List.of(order));

        // when
        boolean result = orderService.hasExistingOrder(275, 61429, LocalDate.of(2026, 3, 25));

        // then
        assertThat(result).isFalse();
    }

    @Test
    void should_check_unpaid_orders_before_paid_orders() {
        // given
        RouteStopsResponse stopsResponse = new RouteStopsResponse();
        stopsResponse.setRouteName("17号线-明珠线-上班");
        given(apiClient.getRouteStops(275, List.of(61429))).willReturn(stopsResponse);

        given(apiClient.getOrders("待支付", 1, 10)).willReturn(List.of());
        given(apiClient.getOrders("已支付", 1, 10)).willReturn(List.of());

        // when
        orderService.hasExistingOrder(275, 61429, LocalDate.of(2026, 3, 25));

        // then
        var inOrder = inOrder(apiClient);
        inOrder.verify(apiClient).getOrders("待支付", 1, 10);
        inOrder.verify(apiClient).getOrders("已支付", 1, 10);
    }

    @Test
    void should_get_route_name_from_route_stops_api() {
        // given
        RouteStopsResponse stopsResponse = new RouteStopsResponse();
        stopsResponse.setRouteName("17号线-明珠线-上班");
        given(apiClient.getRouteStops(275, List.of(61429))).willReturn(stopsResponse);
        given(apiClient.getOrders("待支付", 1, 10)).willReturn(List.of());
        given(apiClient.getOrders("已支付", 1, 10)).willReturn(List.of());

        // when
        orderService.hasExistingOrder(275, 61429, LocalDate.of(2026, 3, 25));

        // then
        verify(apiClient).getRouteStops(275, List.of(61429));
    }

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

        RouteStopsResponse stopsResponse = new RouteStopsResponse();
        stopsResponse.setRouteName("17号线-明珠线-上班");
        given(apiClient.getRouteStops(275, List.of(61429))).willReturn(stopsResponse);
        given(apiClient.getOrders("待支付", 1, 10)).willReturn(List.of());
        given(apiClient.getOrders("已支付", 1, 10)).willReturn(List.of());

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

        RouteStopsResponse stopsResponse = new RouteStopsResponse();
        stopsResponse.setRouteName("17号线-明珠线-上班");
        given(apiClient.getRouteStops(275, List.of(61429))).willReturn(stopsResponse);
        given(apiClient.getOrders("待支付", 1, 10)).willReturn(List.of());
        given(apiClient.getOrders("已支付", 1, 10)).willReturn(List.of());

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
    void should_return_false_when_existing_order() {
        // given
        ScheduleItem schedule = createSchedule(61429, "2026/3/25", "07:40", 1);
        LocalDateTime beforeDeparture = LocalDateTime.of(2026, 3, 25, 7, 0);
        given(properties.getRouteId()).willReturn(275);

        RouteStopsResponse stopsResponse = new RouteStopsResponse();
        stopsResponse.setRouteName("17号线-明珠线-上班");
        given(apiClient.getRouteStops(275, List.of(61429))).willReturn(stopsResponse);

        OrderItem existingOrder = createOrder(572468, "17号线-明珠线-上班", "2026-03-25 07:40:00", "支付成功");
        given(apiClient.getOrders("待支付", 1, 10)).willReturn(List.of());
        given(apiClient.getOrders("已支付", 1, 10)).willReturn(List.of(existingOrder));

        // when
        boolean result = orderService.tryCreateOrder(schedule, beforeDeparture);

        // then
        assertThat(result).isFalse();
        verify(apiClient, never()).createOrder(any());
    }

    @Test
    void should_return_true_when_all_coupons_fail_and_order_without_coupon() {
        // given
        ScheduleItem schedule = createSchedule(61429, "2026/3/25", "07:40", 1);
        LocalDateTime beforeDeparture = LocalDateTime.of(2026, 3, 25, 7, 0);
        given(properties.getRouteId()).willReturn(275);
        given(properties.getBoardingPointId()).willReturn(24);
        given(properties.getAlightingPointId()).willReturn(400);

        RouteStopsResponse stopsResponse = new RouteStopsResponse();
        stopsResponse.setRouteName("17号线-明珠线-上班");
        given(apiClient.getRouteStops(275, List.of(61429))).willReturn(stopsResponse);
        given(apiClient.getOrders("待支付", 1, 10)).willReturn(List.of());
        given(apiClient.getOrders("已支付", 1, 10)).willReturn(List.of());

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
}
