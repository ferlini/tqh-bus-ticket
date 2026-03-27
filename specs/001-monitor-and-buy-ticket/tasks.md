# TASKS-001: 车票监控与自动下单系统 — 任务列表

> 所有任务严格遵循 TDD Red-Green-Refactor：先写测试（RED），再写实现（GREEN）。
> `[P]` 标记表示该任务可与同阶段内的其他 `[P]` 任务并行执行。

---

## Phase 1: 基础设施

### 1.1 项目脚手架

| ID | 任务 | 文件 | 依赖 | 说明 |
|----|------|------|------|------|
| 1.1.1 | 初始化 Spring Boot 3.x Maven 项目 | `pom.xml` | 无 | Java 21, spring-boot-starter-web, spring-boot-starter-test, 无其他额外依赖 |
| 1.1.2 | 创建应用入口类 | `TqhBusTicketApplication.java` | 1.1.1 | `@SpringBootApplication` + `@EnableScheduling` |
| 1.1.3 | 创建 application.yml | `src/main/resources/application.yml` | 1.1.1 | 配置 `tqh.*` 属性默认值（base-url, auth-token, route-id, boarding-point-id, alighting-point-id, monitor-interval） |

### 1.2 配置属性

| ID | 任务 | 文件 | 依赖 | 说明 |
|----|------|------|------|------|
| 1.2.1 | **RED** 编写 TqhProperties 测试 | `TqhPropertiesTest.java` | 1.1.3 | `should_bind_default_values_from_yml`、`should_bind_custom_values` |
| 1.2.2 | **GREEN** 实现 TqhProperties | `TqhProperties.java` | 1.2.1 | `@ConfigurationProperties(prefix = "tqh")`，字段：baseUrl, authToken, routeId, boardingPointId, alightingPointId, monitorInterval |

### 1.3 RestClient 配置

| ID | 任务 | 文件 | 依赖 | 说明 |
|----|------|------|------|------|
| 1.3.1 | **RED** 编写 RestClientConfig 测试 | `RestClientConfigTest.java` | 1.2.2 | `should_create_rest_client_bean`、`should_set_base_url_from_properties` |
| 1.3.2 | **GREEN** 实现 RestClientConfig | `RestClientConfig.java` | 1.3.1 | 创建 RestClient Bean，预配置 baseUrl、公共请求头（X-Auth-Token, User-Agent, Referer, Content-Type），配置超时 |

---

## Phase 2: 外部 API 层

### 2.1 请求/响应模型

> 模型类为纯数据结构（record 或 POJO），无需单独测试。通过 TqhApiClient 测试间接验证序列化/反序列化。

| ID | 任务 | 文件 | 依赖 | 说明 |
|----|------|------|------|------|
| 2.1.1 `[P]` | 创建通用响应包装模型 | `ApiResponse.java` | 1.3.2 | 泛型类，字段：code, msg, data |
| 2.1.2 `[P]` | 创建车次库存模型 | `ScheduleFindRequest.java`, `ScheduleItem.java` | 1.3.2 | Request: route_id; Item: id, date(String), time, price, number |
| 2.1.3 `[P]` | 创建订单列表模型 | `OrderGetRequest.java`, `OrderItem.java`, `OrderDescription.java` | 1.3.2 | Request: status, page, limit; Item: id, route_name, description, trade_state; Description: start_stop, end_stop, date(List) |
| 2.1.4 `[P]` | 创建路线站点模型 | `RouteStopsRequest.java`, `RouteStopsResponse.java`, `StopItem.java` | 1.3.2 | Request: route_id, schedule_ids; Response: route_name, route_id, up(List), down(List); StopItem: stop_id, name, sequence, time |
| 2.1.5 `[P]` | 创建优惠券模型 | `CouponRequest.java`, `CouponItem.java` | 1.3.2 | Request: route_id, schedule_ids, boarding_point_id; Item: id, coupon_category_id, is_use(Map), status(Map), denomination |
| 2.1.6 `[P]` | 创建价格验证模型 | `PriceVerificationRequest.java`, `PriceVerificationResponse.java` | 1.3.2 | Request: route_id, schedule_ids, coupon_ids(嵌套Map), boarding_point_id, alighting_point_id; Response: original_price, discount, final_price |
| 2.1.7 `[P]` | 创建订单创建模型 | `CreateOrderRequest.java`, `CreateOrderResponse.java` | 1.3.2 | Request: route_id, boarding_point_id, alighting_point_id, schedule_ids, coupon_ids(嵌套Map); Response: wx_order_id, is_zero_order |

### 2.2 TqhApiClient

| ID | 任务 | 文件 | 依赖 | 说明 |
|----|------|------|------|------|
| 2.2.1 | **RED** 编写 findSchedules 测试 | `TqhApiClientTest.java` | 2.1.1, 2.1.2 | `should_parse_schedule_response_when_api_returns_success`：验证 Map<String, List<ScheduleItem>> 结构解析; `should_parse_date_format_yyyy_slash_M_slash_d`：验证 "2026/3/24" 正确解析 |
| 2.2.2 | **GREEN** 实现 findSchedules | `TqhApiClient.java` | 2.2.1 | POST /api/v1/route/schedule/find，请求 {"route_id": x}，使用 RestClient |
| 2.2.3 | **RED** 编写 getOrders 测试 | `TqhApiClientTest.java` | 2.1.3, 2.2.2 | `should_return_order_list_when_api_returns_success`：验证 OrderItem 列表解析及 OrderDescription 嵌套结构 |
| 2.2.4 | **GREEN** 实现 getOrders | `TqhApiClient.java` | 2.2.3 | POST /api/v1/order/get，请求 {"status": x, "page": x, "limit": x} |
| 2.2.5 | **RED** 编写 getRouteStops 测试 | `TqhApiClientTest.java` | 2.1.4, 2.2.4 | `should_return_route_name_from_route_stops`：验证 route_name 解析; `should_return_up_and_down_stops`：验证站点列表 |
| 2.2.6 | **GREEN** 实现 getRouteStops | `TqhApiClient.java` | 2.2.5 | POST /api/v1/route/timetable/stops，请求 {"route_id": x, "schedule_ids": [x]} |
| 2.2.7 | **RED** 编写 getCoupons 测试 | `TqhApiClientTest.java` | 2.1.5, 2.2.6 | `should_return_coupon_list_when_api_returns_success`：验证 is_use/status Map 结构解析 |
| 2.2.8 | **GREEN** 实现 getCoupons | `TqhApiClient.java` | 2.2.7 | POST /api/v2/order/coupon |
| 2.2.9 | **RED** 编写 verifyPrice 测试 | `TqhApiClientTest.java` | 2.1.6, 2.2.8 | `should_build_coupon_ids_correctly_for_price_verification`：验证嵌套 Map 序列化正确 |
| 2.2.10 | **GREEN** 实现 verifyPrice | `TqhApiClient.java` | 2.2.9 | POST /api/v2/order/price/verification |
| 2.2.11 | **RED** 编写 createOrder 测试 | `TqhApiClientTest.java` | 2.1.7, 2.2.10 | `should_create_order_and_return_wx_order_id`：验证 wx_order_id 返回 |
| 2.2.12 | **GREEN** 实现 createOrder | `TqhApiClient.java` | 2.2.11 | POST /api/v2/order/create |
| 2.2.13 | **RED** 编写 API 错误处理测试 | `TqhApiClientTest.java` | 2.2.12 | `should_throw_when_api_returns_non_200_code`：验证非 200 code 时抛出异常 |
| 2.2.14 | **GREEN** 实现 API 错误处理 | `TqhApiClient.java` | 2.2.13 | 检查 ApiResponse.code，非 200 抛出自定义异常或 RuntimeException |

---

## Phase 3: 业务逻辑层

### 3.1 DateRangeCalculator

| ID | 任务 | 文件 | 依赖 | 说明 |
|----|------|------|------|------|
| 3.1.1 `[P]` | **RED** 编写 thisWeekDates 测试 | `DateRangeCalculatorTest.java` | 1.1.1 | `should_return_wednesday_to_sunday_when_today_is_wednesday`; `should_return_only_sunday_when_today_is_sunday`; `should_return_monday_to_sunday_when_today_is_monday` |
| 3.1.2 `[P]` | **GREEN** 实现 thisWeekDates | `DateRangeCalculator.java` | 3.1.1 | 今天至本周日的所有日期 |
| 3.1.3 `[P]` | **RED** 编写 nextWeekDates 测试 | `DateRangeCalculatorTest.java` | 3.1.2 | `should_return_next_monday_to_sunday` |
| 3.1.4 `[P]` | **GREEN** 实现 nextWeekDates | `DateRangeCalculator.java` | 3.1.3 | 下周一至下周日 |
| 3.1.5 `[P]` | **RED** 编写 filterPastDates 测试 | `DateRangeCalculatorTest.java` | 3.1.4 | `should_exclude_past_dates`; `should_return_empty_when_all_dates_past` |
| 3.1.6 `[P]` | **GREEN** 实现 filterPastDates | `DateRangeCalculator.java` | 3.1.5 | 过滤掉 today 之前的日期 |

### 3.2 OrderService

| ID | 任务 | 文件 | 依赖 | 说明 |
|----|------|------|------|------|
| 3.2.1 | **RED** 编写 hasExistingOrder 测试 | `OrderServiceTest.java` | 2.2.14 | `should_return_true_when_unpaid_order_exists_for_date_and_route`：route_name 匹配 + date 日期部分匹配 + trade_state="未支付"; `should_return_true_when_paid_order_exists`：trade_state="支付成功"; `should_return_false_when_no_matching_order`; `should_return_false_when_order_exists_but_closed`：trade_state="已关闭" 不算已购票; `should_get_route_name_from_route_stops_api`：验证内部调用 getRouteStops 获取 route_name |
| 3.2.2 | **GREEN** 实现 hasExistingOrder | `OrderService.java` | 3.2.1 | 调用 getRouteStops 获取 route_name → 调用 getOrders → 遍历匹配 route_name + date 日期部分 + trade_state |
| 3.2.3 | **RED** 编写 findUsableCoupons 测试 | `OrderServiceTest.java` | 3.2.2 | `should_filter_usable_coupons_by_is_use_and_status`：仅保留 is_use[scheduleId]=true 且 status[scheduleId]="待使用"; `should_return_empty_list_when_no_usable_coupons` |
| 3.2.4 | **GREEN** 实现 findUsableCoupons | `OrderService.java` | 3.2.3 | 调用 getCoupons → 筛选 |
| 3.2.5 | **RED** 编写 tryVerifyCoupon 测试 | `OrderServiceTest.java` | 3.2.4 | `should_return_first_verified_coupon`; `should_use_second_coupon_when_first_verification_fails`; `should_return_empty_when_all_coupons_fail` |
| 3.2.6 | **GREEN** 实现 tryVerifyCoupon | `OrderService.java` | 3.2.5 | 逐张调用 verifyPrice，code=200 即返回 |
| 3.2.7 | **RED** 编写 placeOrder 测试 | `OrderServiceTest.java` | 3.2.6 | `should_create_order_with_coupon`：coupon_ids 包含优惠券; `should_create_order_without_coupon`：coupon_ids 为空 Map |
| 3.2.8 | **GREEN** 实现 placeOrder | `OrderService.java` | 3.2.7 | 构建 CreateOrderRequest 并调用 createOrder |
| 3.2.9 | **RED** 编写 tryCreateOrder 测试（编排） | `OrderServiceTest.java` | 3.2.8 | `should_return_true_when_order_created_successfully`：完整流程; `should_return_false_when_existing_order`：已购票跳过; `should_return_true_when_all_coupons_fail_and_order_without_coupon` |
| 3.2.10 | **GREEN** 实现 tryCreateOrder | `OrderService.java` | 3.2.9 | 编排 hasExistingOrder → findUsableCoupons → tryVerifyCoupon → placeOrder |

### 3.3 TicketLogService

| ID | 任务 | 文件 | 依赖 | 说明 |
|----|------|------|------|------|
| 3.3.1 `[P]` | **RED** 编写 logTicketPurchase 测试 | `TicketLogServiceTest.java` | 2.2.14 | `should_append_ticket_info_to_log_file`：验证写入 date, route_name, start_stop, end_stop; `should_format_log_with_separator`：验证分割线格式; `should_query_order_by_wx_order_id`：验证通过 wxOrderId 匹配 |
| 3.3.2 `[P]` | **GREEN** 实现 logTicketPurchase | `TicketLogService.java` | 3.3.1 | 调用 getOrders → 匹配 id==wxOrderId → 格式化写入 ./logs/ticket.log |
| 3.3.3 `[P]` | **RED** 编写 clearLog 测试 | `TicketLogServiceTest.java` | 3.3.2 | `should_clear_log_file`：验证文件内容被清空; `should_not_throw_when_log_file_not_exists` |
| 3.3.4 `[P]` | **GREEN** 实现 clearLog | `TicketLogService.java` | 3.3.3 | `@Scheduled(cron = "0 0 10 ? * FRI")`，清空文件 |

### 3.4 TicketMonitorService

| ID | 任务 | 文件 | 依赖 | 说明 |
|----|------|------|------|------|
| 3.4.1 | **RED** 编写 executeMonitorCycle 测试 | `TicketMonitorServiceTest.java` | 3.2.10, 3.3.4 | `should_intersect_target_dates_with_available_schedules`; `should_skip_dates_with_zero_tickets`; `should_call_order_service_for_each_available_date`; `should_continue_processing_next_date_when_one_fails`; `should_log_ticket_after_successful_order` |
| 3.4.2 | **GREEN** 实现 executeMonitorCycle | `TicketMonitorService.java` | 3.4.1 | 查询库存 → 筛选有票 → 交集 → 逐日期调用 OrderService + TicketLogService |
| 3.4.3 | **RED** 编写 startMonitor/stopMonitor 测试 | `TicketMonitorServiceTest.java` | 3.4.2 | `should_schedule_monitor_cycle_with_fixed_delay`; `should_stop_existing_monitor_before_starting_new`; `should_continue_monitoring_when_api_fails` |
| 3.4.4 | **GREEN** 实现 startMonitor/stopMonitor | `TicketMonitorService.java` | 3.4.3 | ScheduledExecutorService + scheduleWithFixedDelay，停止旧任务再启动新任务 |

---

## Phase 4: 控制器层

### 4.1 MonitorController

| ID | 任务 | 文件 | 依赖 | 说明 |
|----|------|------|------|------|
| 4.1.1 `[P]` | 创建 MonitorDatesRequest 模型 | `MonitorDatesRequest.java` | 3.4.4 | 字段：dates(List<String>)，用于 /monitor/dates 请求体 |
| 4.1.2 | **RED** 编写 monitorThisWeek 测试 | `MonitorControllerTest.java` | 4.1.1 | `should_return_this_week_dates_when_post_this_week`：验证返回 status=started, monitorDates 包含今天至周日 |
| 4.1.3 | **GREEN** 实现 monitorThisWeek | `MonitorController.java` | 4.1.2 | POST /monitor/this-week → DateRangeCalculator.thisWeekDates → TicketMonitorService.startMonitor |
| 4.1.4 | **RED** 编写 monitorNextWeek 测试 | `MonitorControllerTest.java` | 4.1.3 | `should_return_next_week_dates_when_post_next_week` |
| 4.1.5 | **GREEN** 实现 monitorNextWeek | `MonitorController.java` | 4.1.4 | POST /monitor/next-week → DateRangeCalculator.nextWeekDates → startMonitor |
| 4.1.6 | **RED** 编写 monitorDates 测试 | `MonitorControllerTest.java` | 4.1.5 | `should_accept_custom_dates_when_post_dates`; `should_reject_invalid_date_format`; `should_filter_past_dates_from_request` |
| 4.1.7 | **GREEN** 实现 monitorDates | `MonitorController.java` | 4.1.6 | POST /monitor/dates → 解析日期 → filterPastDates → startMonitor |

---

## 任务总览

| 阶段 | 任务数 | 说明 |
|------|--------|------|
| Phase 1: 基础设施 | 7 | 项目脚手架 + 配置属性 + RestClient |
| Phase 2: 外部 API 层 | 21 | 14 个模型文件 + 7 对 TqhApiClient 测试/实现 |
| Phase 3: 业务逻辑层 | 18 | DateRangeCalculator(6) + OrderService(10) + TicketLogService(4) + TicketMonitorService(4) 含 6 对测试/实现 |
| Phase 4: 控制器层 | 7 | MonitorDatesRequest(1) + 3 对端点测试/实现 |
| **合计** | **53** | |

---

## 依赖关系图（简化）

```
Phase 1
  1.1.1 → 1.1.2
  1.1.1 → 1.1.3 → 1.2.1 → 1.2.2 → 1.3.1 → 1.3.2

Phase 2
  1.3.2 → [2.1.1~2.1.7 并行] → 2.2.1 → 2.2.2 → ... → 2.2.14

Phase 3 (3.1 与 3.3 可并行)
  2.2.14 ──┬─→ 3.1.1 → ... → 3.1.6  ─────────────────────┐
           ├─→ 3.2.1 → ... → 3.2.10 ──┐                   │
           └─→ 3.3.1 → ... → 3.3.4  ──┤                   │
                                        └─→ 3.4.1 → ... → 3.4.4

Phase 4
  3.4.4 + 3.1.6 → 4.1.1 → 4.1.2 → ... → 4.1.7
```
