package com.tqh.bus.ticket.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tqh.bus.ticket.common.BusinessException;
import com.tqh.bus.ticket.integration.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class TqhApiClient {

    private static final Logger log = LoggerFactory.getLogger(TqhApiClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public TqhApiClient(RestClient restClient) {
        this.restClient = restClient;
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, List<ScheduleItem>> findSchedules(int routeId) {
        ApiResponse<Object> response = postForResponse(
                "/api/v1/route/schedule/find", new ScheduleFindRequest(routeId));
        return objectMapper.convertValue(response.getData(),
                new TypeReference<Map<String, List<ScheduleItem>>>() {});
    }

    public List<OrderItem> getOrders(String status, int page, int limit) {
        ApiResponse<Object> response = postForResponse(
                "/api/v1/order/get", new OrderGetRequest(status, page, limit));
        return objectMapper.convertValue(response.getData(),
                new TypeReference<List<OrderItem>>() {});
    }

    public RouteStopsResponse getRouteStops(int routeId, List<Integer> scheduleIds) {
        ApiResponse<Object> response = postForResponse(
                "/api/v1/route/timetable/stops", new RouteStopsRequest(routeId, scheduleIds));
        return objectMapper.convertValue(response.getData(), RouteStopsResponse.class);
    }

    public List<CouponItem> getCoupons(int routeId, List<Integer> scheduleIds, int boardingPointId) {
        ApiResponse<Object> response = postForResponse(
                "/api/v2/order/coupon", new CouponRequest(routeId, scheduleIds, boardingPointId));
        return objectMapper.convertValue(response.getData(),
                new TypeReference<List<CouponItem>>() {});
    }

    public PriceVerificationResponse verifyPrice(PriceVerificationRequest request) {
        ApiResponse<Object> response = postForResponse(
                "/api/v2/order/price/verification", request);
        return objectMapper.convertValue(response.getData(), PriceVerificationResponse.class);
    }

    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        ApiResponse<Object> response = postForResponse(
                "/api/v2/order/create", request);
        return objectMapper.convertValue(response.getData(), CreateOrderResponse.class);
    }

    private <T> ApiResponse<T> postForResponse(String uri, Object body) {
        if (log.isDebugEnabled()) {
            log.debug(">>> POST {} | 请求报文: {}", uri, toJson(body));
        }
        ApiResponse<Object> response = restClient.post()
                .uri(uri)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<Object>>() {});
        checkResponse(response);
        if (log.isDebugEnabled()) {
            log.debug("<<< POST {} | 响应报文: {}", uri, toJson(response));
        }
        @SuppressWarnings("unchecked")
        ApiResponse<T> typed = (ApiResponse<T>) response;
        return typed;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    /**
     * 检查 API 响应是否成功，失败时抛出 BusinessException。
     *
     * @param response API 响应对象
     * @throws BusinessException 当响应为空、code 不等于 200 时抛出
     */
    private void checkResponse(ApiResponse<?> response) {
        if (response == null) {
            throw new BusinessException(500, "API 调用失败: 响应为空");
        }
        if (response.getCode() != 200) {
            throw new BusinessException(response.getCode(),
                    "API 调用失败: " + response.getMsg() + " (code=" + response.getCode() + ")");
        }
    }
}
