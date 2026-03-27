package com.tqh.bus.ticket.service;

import com.tqh.bus.ticket.common.UnpaidOrderException;
import com.tqh.bus.ticket.config.TqhProperties;
import com.tqh.bus.ticket.integration.TqhApiClient;
import com.tqh.bus.ticket.integration.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final DateTimeFormatter SCHEDULE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/M/d");

    // API 查询配置常量
    private static final int DEFAULT_ORDER_PAGE = 1;
    private static final int DEFAULT_ORDER_PAGE_SIZE = 10;
    private static final String ALL_ORDERS_STATUS = "";

    private final TqhApiClient apiClient;
    private final TqhProperties properties;
    private int lastCreatedOrderId;

    public OrderService(TqhApiClient apiClient, TqhProperties properties) {
        this.apiClient = apiClient;
        this.properties = properties;
    }

    public boolean tryCreateOrder(ScheduleItem schedule) {
        return tryCreateOrder(schedule, LocalDateTime.now());
    }

    boolean tryCreateOrder(ScheduleItem schedule, LocalDateTime now) {
        int scheduleId = schedule.getId();
        LocalDate date = LocalDate.parse(schedule.getDate(), SCHEDULE_DATE_FORMAT);
        LocalTime time = LocalTime.parse(schedule.getTime());
        LocalDateTime departureTime = LocalDateTime.of(date, time);

        if (!now.isBefore(departureTime)) {
            log.info("车次 {} 已于 {} 出发，跳过", scheduleId, departureTime);
            return false;
        }
        log.debug("开始下单流程: scheduleId={}, 出发时间={}", scheduleId, departureTime);

        if (hasExistingOrder(properties.getRouteId(), scheduleId, date)) {
            log.info("日期 {} 已有订单，跳过", date);
            return false;
        }

        List<CouponItem> coupons = findUsableCoupons(
                properties.getRouteId(), scheduleId, properties.getBoardingPointId());
        log.debug("日期 {} 可用优惠券: {}张", date, coupons.size());

        Optional<CouponItem> verifiedCoupon = tryVerifyCoupon(coupons, scheduleId);
        log.debug("日期 {} 使用优惠券: {}", date,
                verifiedCoupon.map(c -> "id=" + c.getId() + ", 面额=" + c.getDenomination()).orElse("无"));

        CreateOrderResponse response = placeOrder(scheduleId, verifiedCoupon);
        lastCreatedOrderId = response.getWxOrderId();
        log.info("日期 {} 下单成功，wx_order_id={}", date, response.getWxOrderId());
        return true;
    }

    public int getLastCreatedOrderId() {
        return lastCreatedOrderId;
    }

    boolean hasExistingOrder(int routeId, int scheduleId, LocalDate date) {
        RouteStopsResponse stopsResponse = apiClient.getRouteStops(routeId, List.of(scheduleId));
        String routeName = stopsResponse.getRouteName();
        log.debug("检查已购票: routeName={}, 日期={}", routeName, date);

        List<OrderItem> unpaidOrders = apiClient.getOrders("待支付", DEFAULT_ORDER_PAGE, DEFAULT_ORDER_PAGE_SIZE);
        log.debug("待支付订单: {}条", unpaidOrders.size());
        if (!unpaidOrders.isEmpty()) {
            String unpaidInfo = formatUnpaidOrderInfo(unpaidOrders);
            throw new UnpaidOrderException(unpaidInfo);
        }

        List<OrderItem> paidOrders = apiClient.getOrders("已支付", DEFAULT_ORDER_PAGE, DEFAULT_ORDER_PAGE_SIZE);
        log.debug("已支付订单: {}条", paidOrders.size());
        if (paidOrders.stream().anyMatch(order -> matchesOrder(order, routeName, date))) {
            log.debug("发现已支付的匹配订单，跳过日期 {}", date);
            return true;
        }

        log.debug("日期 {} 无已有订单，可以下单", date);
        return false;
    }

    List<CouponItem> findUsableCoupons(int routeId, int scheduleId, int boardingPointId) {
        List<CouponItem> allCoupons = apiClient.getCoupons(routeId, List.of(scheduleId), boardingPointId);
        String scheduleIdStr = String.valueOf(scheduleId);

        return allCoupons.stream()
                .filter(coupon -> isUsableCoupon(coupon, scheduleIdStr))
                .toList();
    }

    Optional<CouponItem> tryVerifyCoupon(List<CouponItem> coupons, int scheduleId) {
        for (CouponItem coupon : coupons) {
            try {
                PriceVerificationRequest request = buildVerificationRequest(scheduleId, coupon);
                apiClient.verifyPrice(request);
                log.info("优惠券 {} 验证成功", coupon.getId());
                return Optional.of(coupon);
            } catch (Exception e) {
                // 优惠券验证失败是可预期的，继续尝试下一张
                log.debug("优惠券 {} 验证失败，尝试下一张: {}", coupon.getId(), e.getMessage());
            }
        }
        return Optional.empty();
    }

    CreateOrderResponse placeOrder(int scheduleId, Optional<CouponItem> coupon) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setRouteId(properties.getRouteId());
        request.setBoardingPointId(properties.getBoardingPointId());
        request.setAlightingPointId(properties.getAlightingPointId());
        request.setScheduleIds(List.of(scheduleId));
        request.setCouponIds(buildCouponIds(scheduleId, coupon));
        return apiClient.createOrder(request);
    }

    private boolean matchesOrder(OrderItem order, String routeName, LocalDate date) {
        if (!routeName.equals(order.getRouteName())) {
            return false;
        }

        return order.getDescription().getDate().stream()
                .anyMatch(dateStr -> dateStr.startsWith(date.toString()));
    }

    private boolean isUsableCoupon(CouponItem coupon, String scheduleIdStr) {
        Boolean canUse = coupon.getIsUse().get(scheduleIdStr);
        String status = coupon.getStatus().get(scheduleIdStr);
        return Boolean.TRUE.equals(canUse) && "待使用".equals(status);
    }

    private PriceVerificationRequest buildVerificationRequest(int scheduleId, CouponItem coupon) {
        PriceVerificationRequest request = new PriceVerificationRequest();
        request.setRouteId(properties.getRouteId());
        request.setScheduleIds(List.of(scheduleId));
        request.setBoardingPointId(properties.getBoardingPointId());
        request.setAlightingPointId(properties.getAlightingPointId());

        String scheduleIdStr = String.valueOf(scheduleId);
        String categoryIdStr = String.valueOf(coupon.getCouponCategoryId());
        request.setCouponIds(Map.of(scheduleIdStr, Map.of(categoryIdStr, coupon.getId())));
        return request;
    }

    private String formatUnpaidOrderInfo(List<OrderItem> unpaidOrders) {
        StringBuilder sb = new StringBuilder();
        for (OrderItem order : unpaidOrders) {
            String date = order.getDescription().getDate().get(0);
            sb.append("你有待支付的订单，乘车日期: ").append(date)
              .append("，路线: ").append(order.getRouteName())
              .append("，车票监控暂停，请付款后重新开启车票监控\n");
        }
        return sb.toString().trim();
    }

    private Map<String, Map<String, Integer>> buildCouponIds(int scheduleId, Optional<CouponItem> coupon) {
        if (coupon.isEmpty()) {
            return Map.of();
        }
        CouponItem c = coupon.get();
        String scheduleIdStr = String.valueOf(scheduleId);
        String categoryIdStr = String.valueOf(c.getCouponCategoryId());
        return Map.of(scheduleIdStr, Map.of(categoryIdStr, c.getId()));
    }
}
