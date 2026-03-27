package com.tqh.bus.ticket.integration;

import com.tqh.bus.ticket.common.BusinessException;
import com.tqh.bus.ticket.integration.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TqhApiClientTest {

    private TqhApiClient apiClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.com");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        apiClient = new TqhApiClient(builder.build());
    }

    // === findSchedules (2.2.1) ===

    @Test
    void should_parse_schedule_response_when_api_returns_success() {
        String responseJson = """
                {
                  "code": 200,
                  "msg": "查询成功",
                  "data": {
                    "07:40": [
                      {"id": 61429, "date": "2026/3/24", "time": "07:40", "price": 13.0, "number": 1},
                      {"id": 61430, "date": "2026/3/25", "time": "07:40", "price": 13.0, "number": 0}
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo("https://api.com/api/v1/route/schedule/find"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        Map<String, List<ScheduleItem>> data = apiClient.findSchedules(275);

        assertThat(data).containsKey("07:40");
        List<ScheduleItem> items = data.get("07:40");
        assertThat(items).hasSize(2);
        assertThat(items.get(0).getId()).isEqualTo(61429);
        assertThat(items.get(0).getNumber()).isEqualTo(1);
        assertThat(items.get(1).getNumber()).isEqualTo(0);

        mockServer.verify();
    }

    @Test
    void should_parse_date_format_yyyy_slash_M_slash_d() {
        String responseJson = """
                {
                  "code": 200,
                  "msg": "查询成功",
                  "data": {
                    "07:40": [
                      {"id": 61429, "date": "2026/3/24", "time": "07:40", "price": 13.0, "number": 1}
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo("https://api.com/api/v1/route/schedule/find"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        Map<String, List<ScheduleItem>> data = apiClient.findSchedules(275);

        assertThat(data.get("07:40").get(0).getDate()).isEqualTo("2026/3/24");

        mockServer.verify();
    }

    // === getOrders (2.2.3) ===

    @Test
    void should_return_order_list_when_api_returns_success() {
        String responseJson = """
                {
                  "code": 200,
                  "msg": "操作成功",
                  "data": [
                    {
                      "id": 572468,
                      "route_name": "17号线-明珠线-上班",
                      "description": {
                        "start_stop": "长沙圩①",
                        "end_stop": "科创中心西门",
                        "date": ["2026-03-24 07:40:00"]
                      },
                      "trade_state": "支付成功"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://api.com/api/v1/order/get"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<OrderItem> orders = apiClient.getOrders("", 1, 10);

        assertThat(orders).hasSize(1);
        OrderItem order = orders.get(0);
        assertThat(order.getId()).isEqualTo(572468);
        assertThat(order.getRouteName()).isEqualTo("17号线-明珠线-上班");
        assertThat(order.getTradeState()).isEqualTo("支付成功");
        assertThat(order.getDescription().getStartStop()).isEqualTo("长沙圩①");
        assertThat(order.getDescription().getEndStop()).isEqualTo("科创中心西门");
        assertThat(order.getDescription().getDate()).containsExactly("2026-03-24 07:40:00");

        mockServer.verify();
    }

    // === getRouteStops (2.2.5) ===

    @Test
    void should_return_route_name_from_route_stops() {
        String responseJson = """
                {
                  "code": 200,
                  "msg": "操作成功",
                  "data": {
                    "route_id": 275,
                    "route_name": "17号线-明珠线-上班",
                    "up": [
                      {"stop_id": 24, "name": "长沙圩①", "sequence": 10, "time": "07:43"}
                    ],
                    "down": [
                      {"stop_id": 400, "name": "科创中心西门", "sequence": 200, "time": "08:30"}
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo("https://api.com/api/v1/route/timetable/stops"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        RouteStopsResponse stopsResponse = apiClient.getRouteStops(275, List.of(61429));

        assertThat(stopsResponse.getRouteName()).isEqualTo("17号线-明珠线-上班");
        assertThat(stopsResponse.getRouteId()).isEqualTo(275);
        assertThat(stopsResponse.getUp()).hasSize(1);
        assertThat(stopsResponse.getUp().get(0).getName()).isEqualTo("长沙圩①");
        assertThat(stopsResponse.getDown()).hasSize(1);
        assertThat(stopsResponse.getDown().get(0).getName()).isEqualTo("科创中心西门");

        mockServer.verify();
    }

    // === getCoupons (2.2.7) ===

    @Test
    void should_return_coupon_list_when_api_returns_success() {
        String responseJson = """
                {
                  "code": 200,
                  "msg": "操作成功",
                  "data": [
                    {
                      "id": 8317178,
                      "coupon_category_id": 2,
                      "coupon_name": "粤澳合作区政府券",
                      "denomination": 10.0,
                      "is_use": {"61429": true},
                      "status": {"61429": "待使用"}
                    },
                    {
                      "id": 8190582,
                      "coupon_category_id": 2,
                      "coupon_name": "粤澳合作区政府券",
                      "denomination": 23.0,
                      "is_use": {"61429": false},
                      "status": {"61429": "不可使用"}
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://api.com/api/v2/order/coupon"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<CouponItem> coupons = apiClient.getCoupons(275, List.of(61429), 24);

        assertThat(coupons).hasSize(2);
        CouponItem usable = coupons.get(0);
        assertThat(usable.getId()).isEqualTo(8317178);
        assertThat(usable.getIsUse().get("61429")).isTrue();
        assertThat(usable.getStatus().get("61429")).isEqualTo("待使用");
        CouponItem unusable = coupons.get(1);
        assertThat(unusable.getIsUse().get("61429")).isFalse();
        assertThat(unusable.getStatus().get("61429")).isEqualTo("不可使用");

        mockServer.verify();
    }

    // === verifyPrice (2.2.9) ===

    @Test
    void should_build_coupon_ids_correctly_for_price_verification() {
        String responseJson = """
                {
                  "code": 200,
                  "msg": "操作成功",
                  "data": {
                    "original_price": 13.0,
                    "discount": 10.0,
                    "final_price": 3.0
                  }
                }
                """;

        mockServer.expect(requestTo("https://api.com/api/v2/order/price/verification"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        PriceVerificationRequest request = new PriceVerificationRequest();
        request.setRouteId(275);
        request.setScheduleIds(List.of(61429));
        request.setCouponIds(Map.of("61429", Map.of("2", 8317178)));
        request.setBoardingPointId(24);
        request.setAlightingPointId(400);

        PriceVerificationResponse result = apiClient.verifyPrice(request);

        assertThat(result.getOriginalPrice()).isEqualTo(13.0);
        assertThat(result.getDiscount()).isEqualTo(10.0);
        assertThat(result.getFinalPrice()).isEqualTo(3.0);

        mockServer.verify();
    }

    // === createOrder (2.2.11) ===

    @Test
    void should_create_order_and_return_wx_order_id() {
        String responseJson = """
                {
                  "code": 200,
                  "msg": "购票成功",
                  "data": {
                    "wx_order_id": 572468,
                    "is_zero_order": false
                  }
                }
                """;

        mockServer.expect(requestTo("https://api.com/api/v2/order/create"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        CreateOrderRequest request = new CreateOrderRequest();
        request.setRouteId(275);
        request.setBoardingPointId(24);
        request.setAlightingPointId(400);
        request.setScheduleIds(List.of(61429));
        request.setCouponIds(Map.of("61429", Map.of("2", 8317178)));

        CreateOrderResponse result = apiClient.createOrder(request);

        assertThat(result.getWxOrderId()).isEqualTo(572468);
        assertThat(result.isZeroOrder()).isFalse();

        mockServer.verify();
    }

    // === error handling (2.2.13) ===

    @Test
    void should_throw_when_api_returns_non_200_code() {
        String responseJson = """
                {
                  "code": 401,
                  "msg": "认证失败",
                  "data": null
                }
                """;

        mockServer.expect(requestTo("https://api.com/api/v1/route/schedule/find"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> apiClient.findSchedules(275))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("认证失败");

        mockServer.verify();
    }
}
