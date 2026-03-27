# PLAN-001: 车票监控与自动下单系统 — 技术实现方案

> 基于 `spec.md` 需求规格，遵循 `constitution.md` 核心原则

---

## 1. 技术上下文

### 1.1 技术选型

| 维度 | 选型 | 说明 |
|------|------|------|
| 语言 | Java 21 | 使用 Virtual Threads 简化异步监控 |
| Web 框架 | Spring Boot 3.x | 提供 REST API、调度、配置管理 |
| HTTP 客户端 | RestClient | Spring 6.1+ 内置，同步阻塞模型，简洁直观 |
| 序列化 | Jackson | Spring Boot 自带，零额外依赖 |
| 构建工具 | Maven | 标准 Java 构建 |
| 测试框架 | JUnit 5 + Mockito | 宪法要求 TDD |

### 1.2 不引入的技术（简单性原则）

| 排除项 | 理由 |
|--------|------|
| WebFlux / Reactor | 单用户单路线监控，无高并发需求，同步模型足够 |
| 消息队列 | 无分布式需求，内存调度即可 |
| 数据库 | 无持久化需求，日志用文件，状态通过 API 实时查询 |
| OkHttp / Apache HttpClient | RestClient 已满足需求，不引入额外依赖 |

---

## 2. 合宪性审查

### 2.1 简单性原则 (Simplicity)

| 检查项 | 合规状态 | 说明 |
|--------|---------|------|
| SOLID 底线 | 通过 | 每个类单一职责；无为单一实现创建的接口 |
| 无不必要抽象 | 通过 | 不引入策略模式、工厂模式等；`TqhApiClient` 是唯一的外部 API 封装，不再抽象接口层 |
| 无非必需依赖 | 通过 | 仅使用 Spring Boot Starter Web，RestClient 和 Jackson 均为内置组件 |

### 2.2 测试先行原则 (Test-First)

| 检查项 | 合规状态 | 说明 |
|--------|---------|------|
| TDD 工作流 | 强制执行 | 每个类的实现必须先写失败测试，再写最小实现 |
| 测试可行性 | 通过 | `TqhApiClient` 通过 MockRestServiceServer 测试；业务逻辑通过 Mockito mock `TqhApiClient` 测试 |
| 测试命名 | 强制 | 使用 `should_xxx_when_yyy` 格式 |

**TDD 实施顺序**（由内向外）：

```
1. TqhApiClient       — 验证 HTTP 请求构建和响应解析
2. DateRangeCalculator — 验证日期范围计算逻辑
3. OrderService        — 验证下单流程（优惠券尝试、跳过已购票）
4. TicketMonitorService — 验证监控循环编排
5. TicketLogService    — 验证日志写入格式
6. MonitorController   — 验证 REST 端点参数校验和响应
```

### 2.3 明确性原则 (Clarity)

| 检查项 | 合规状态 | 说明 |
|--------|---------|------|
| 命名即文档 | 通过 | 类名、方法名直接表达业务意图（如 `findAvailableSchedules`、`hasExistingOrder`） |
| 无魔法数字 | 通过 | 所有配置值通过 `@ConfigurationProperties` 管理 |
| 圈复杂度 ≤ 10 | 通过 | 通过提取方法控制每个方法的分支数 |
| 嵌套 ≤ 3 层 | 通过 | 循环内逻辑提取为独立方法 |

---

## 3. 项目结构

```
src/
├── main/
│   ├── java/com/tqh/bus/ticket/
│   │   ├── TqhBusTicketApplication.java
│   │   │
│   │   ├── config/
│   │   │   ├── TqhProperties.java              # 配置属性（route_id, token 等）
│   │   │   └── RestClientConfig.java            # RestClient Bean 配置
│   │   │
│   │   ├── controller/
│   │   │   └── MonitorController.java           # REST 端点（/monitor/*）
│   │   │
│   │   ├── service/
│   │   │   ├── TicketMonitorService.java        # 监控调度与循环编排
│   │   │   ├── OrderService.java                # 下单流程（优惠券尝试、验证、创建）
│   │   │   ├── DateRangeCalculator.java         # 日期范围计算（本周/下周）
│   │   │   └── TicketLogService.java            # 购票日志写入与清理
│   │   │
│   │   └── integration/
│   │       ├── TqhApiClient.java                # 第三方 API 封装（所有 HTTP 调用）
│   │       ├── model/
│   │       │   ├── ScheduleFindRequest.java     # 查询车次库存 — 请求
│   │       │   ├── ScheduleFindResponse.java    # 查询车次库存 — 响应
│   │       │   ├── ScheduleItem.java            # 单个车次信息（id, date, number...）
│   │       │   ├── OrderGetRequest.java         # 订单列表 — 请求
│   │       │   ├── OrderGetResponse.java        # 订单列表 — 响应
│   │       │   ├── OrderItem.java               # 单个订单信息
│   │       │   ├── OrderDescription.java        # 订单描述（start_stop, end_stop, date）
│   │       │   ├── CouponRequest.java           # 查询优惠券 — 请求
│   │       │   ├── CouponResponse.java          # 查询优惠券 — 响应
│   │       │   ├── CouponItem.java              # 单张优惠券信息
│   │       │   ├── RouteStopsRequest.java        # 路线站点 — 请求
│   │       │   ├── RouteStopsResponse.java       # 路线站点 — 响应
│   │       │   ├── StopItem.java                 # 单个站点信息
│   │       │   ├── PriceVerificationRequest.java # 验证订单价格 — 请求
│   │       │   ├── PriceVerificationResponse.java# 验证订单价格 — 响应
│   │       │   ├── CreateOrderRequest.java      # 创建订单 — 请求
│   │       │   ├── CreateOrderResponse.java     # 创建订单 — 响应
│   │       │   └── ApiResponse.java             # 通用响应包装（code, msg, data）
│   │       └──
│   │
│   └── resources/
│       └── application.yml                       # 配置文件
│
└── test/
    └── java/com/tqh/bus/ticket/
        ├── config/
        │   └── TqhPropertiesTest.java
        ├── controller/
        │   └── MonitorControllerTest.java
        ├── service/
        │   ├── TicketMonitorServiceTest.java
        │   ├── OrderServiceTest.java
        │   ├── DateRangeCalculatorTest.java
        │   └── TicketLogServiceTest.java
        └── integration/
            └── TqhApiClientTest.java
```

---

## 4. 核心类设计

### 4.1 配置层 (`config`)

#### `TqhProperties`

```java
@ConfigurationProperties(prefix = "tqh")
public class TqhProperties {
    private String baseUrl;          // API 基础地址
    private String authToken;        // X-Auth-Token
    private int routeId;             // 默认 275
    private int boardingPointId;     // 默认 24
    private int alightingPointId;    // 默认 400
    private int monitorInterval;     // 默认 30（秒）
}
```

#### `application.yml`

```yaml
tqh:
  base-url: https://api.com
  auth-token: "手动配置"
  route-id: 275
  boarding-point-id: 24
  alighting-point-id: 400
  monitor-interval: 30
```

#### `RestClientConfig`

- 创建 `RestClient` Bean，预配置 `baseUrl`、公共请求头（`X-Auth-Token`、`User-Agent`、`Referer`、`Content-Type`）
- 配置连接超时和读取超时

### 4.2 外部 API 层 (`integration`)

#### `TqhApiClient`

职责：封装所有第三方 API 调用，屏蔽 HTTP 细节。

```java
@Component
public class TqhApiClient {

    // 查询车次库存
    // POST /api/v1/route/schedule/find
    // 请求: {"route_id": 275}
    // 响应: data 为 Map<String, List<ScheduleItem>>，key 为时间如 "07:40"
    public ScheduleFindResponse findSchedules(int routeId);

    // 查询订单列表
    // POST /api/v1/order/get
    // 请求: {"status": "", "page": 1, "limit": 10}
    public OrderGetResponse getOrders(String status, int page, int limit);

    // 查询路线站点（获取 route_name）
    // POST /api/v1/route/timetable/stops
    // 请求: {"route_id": 275, "schedule_ids": [61429]}
    // 响应: data 包含 route_name、up（上车站列表）、down（下车站列表）
    public RouteStopsResponse getRouteStops(int routeId, List<Integer> scheduleIds);

    // 查询可用优惠券
    // POST /api/v2/order/coupon
    // 请求: {"route_id": 275, "schedule_ids": [61429], "boarding_point_id": 24}
    public CouponResponse getCoupons(int routeId, List<Integer> scheduleIds, int boardingPointId);

    // 验证订单价格
    // POST /api/v2/order/price/verification
    public PriceVerificationResponse verifyPrice(PriceVerificationRequest request);

    // 创建订单
    // POST /api/v2/order/create
    public CreateOrderResponse createOrder(CreateOrderRequest request);
}
```

#### 响应模型关键说明

**`ScheduleFindResponse.data`** 的 JSON 结构为 `Map<String, List<ScheduleItem>>`：

```json
{
  "07:40": [
    {"id": 61429, "date": "2026/3/24", "time": "07:40", "number": 1, "price": 13.0}
  ]
}
```

- `date` 格式为 `"yyyy/M/d"`，需解析为 `LocalDate`

**`CouponItem`** 的 `is_use` 和 `status` 为 `Map<String, Object>`，key 为 schedule_id 字符串：

```json
{"is_use": {"61429": true}, "status": {"61429": "待使用"}}
```

**`CreateOrderRequest.coupon_ids`** 的结构为嵌套 Map：

```json
{"61429": {"2": 8317178}}
```

- 外层 key: schedule_id（String）
- 内层 key: coupon_category_id（String）
- 内层 value: coupon_id（Integer）

### 4.3 业务逻辑层 (`service`)

#### `DateRangeCalculator`

职责：计算监控日期范围，纯函数，无外部依赖。

```java
@Component
public class DateRangeCalculator {

    // 本周：今天至本周日（排除过去日期）
    public List<LocalDate> thisWeekDates(LocalDate today);

    // 下周：下周一至下周日
    public List<LocalDate> nextWeekDates(LocalDate today);

    // 过滤：排除过去日期
    public List<LocalDate> filterPastDates(List<LocalDate> dates, LocalDate today);
}
```

#### `OrderService`

职责：执行单个日期的下单流程。

```java
@Component
public class OrderService {

    // 对单个有票车次执行完整的下单流程
    // 返回 true 表示下单成功，false 表示跳过或失败
    public boolean tryCreateOrder(ScheduleItem schedule);

    // Step 2: 检查指定日期+路线是否已有订单
    // 内部通过「路线站点」接口查询 route_name，再用 route_name + description.date 日期部分匹配订单
    // trade_state 为 "未支付" 或 "支付成功" 视为已购票
    boolean hasExistingOrder(int routeId, int scheduleId, LocalDate date);

    // Step 3: 查询并筛选可用优惠券
    // 筛选条件：is_use[scheduleId] == true 且 status[scheduleId] == "待使用"
    List<CouponItem> findUsableCoupons(int routeId, int scheduleId, int boardingPointId);

    // Step 4: 依次尝试优惠券验证价格
    // 返回验证通过的优惠券，全部失败返回 Optional.empty()
    Optional<CouponItem> tryVerifyCoupon(List<CouponItem> coupons, int scheduleId);

    // Step 5: 创建订单
    CreateOrderResponse placeOrder(int scheduleId, Optional<CouponItem> coupon);
}
```

**`hasExistingOrder` 的 route_name 获取方式**：

- 需求中检查已购票需要 `route_name`，但当前上下文中只有 `route_id`
- **方案**：在 `hasExistingOrder` 内部调用「路线站点」接口（`/api/v1/route/timetable/stops`），从响应的 `route_name` 字段获取路线名称，再用该名称匹配订单列表

#### `TicketMonitorService`

职责：监控调度与循环编排。

```java
@Component
public class TicketMonitorService {

    // 启动监控任务
    // 1. 计算目标日期范围
    // 2. 启动定时轮询（ScheduledExecutorService）
    // 3. 每轮：查询库存 → 筛选有票日期 → 与目标日期取交集 → 对每个日期调用 OrderService
    public void startMonitor(List<LocalDate> targetDates);

    // 停止当前监控任务
    public void stopMonitor();

    // 单轮监控逻辑
    void executeMonitorCycle(List<LocalDate> targetDates);
}
```

**监控机制**：
- 使用 `ScheduledExecutorService`（单线程），配合 Virtual Thread 执行
- `startMonitor` 被调用时，若已有运行中的监控任务，先停止旧任务再启动新任务
- 每轮结束后等待配置的间隔时间（默认 30 秒）再启动下一轮
- 使用 `scheduleWithFixedDelay`（非 `scheduleAtFixedRate`），确保上一轮完成后再计时

#### `TicketLogService`

职责：购票日志的写入与定时清理。

```java
@Component
public class TicketLogService {

    // 创建订单成功后，查询订单详情并写入日志
    // 通过 wx_order_id 在订单列表中匹配
    public void logTicketPurchase(int wxOrderId);

    // 每周五 10:00 清空日志文件
    @Scheduled(cron = "0 0 10 ? * FRI")
    public void clearLog();
}
```

**日志格式**：

```
----------------------------------------
日期: 2026-03-24 07:40:00
路线: 17号线-明珠线-上班
上车站: 长沙圩①
下车站: 科创中心西门
----------------------------------------
```

- 使用 `java.nio.file.Files` 追加写入，不引入额外日志框架
- 日志路径：`./logs/ticket.log`

### 4.4 控制器层 (`controller`)

#### `MonitorController`

```java
@RestController
@RequestMapping("/monitor")
public class MonitorController {

    // POST /monitor/this-week
    // 计算本周日期范围，启动监控
    @PostMapping("/this-week")
    public ResponseEntity<Map<String, Object>> monitorThisWeek();

    // POST /monitor/next-week
    // 计算下周日期范围，启动监控
    @PostMapping("/next-week")
    public ResponseEntity<Map<String, Object>> monitorNextWeek();

    // POST /monitor/dates
    // 请求体: {"dates": ["2026-03-25", "2026-03-26"]}
    // 校验日期格式，启动监控
    @PostMapping("/dates")
    public ResponseEntity<Map<String, Object>> monitorDates(@RequestBody MonitorDatesRequest request);
}
```

**响应格式**：

```json
{
  "status": "started",
  "monitorDates": ["2026-03-25", "2026-03-26", "2026-03-27"],
  "interval": 30
}
```

---

## 5. 关键流程时序

### 5.1 单轮监控流程

```
MonitorCycle
  │
  ├─ 1. TqhApiClient.findSchedules(routeId)
  │     → 获取所有车次库存
  │     → 筛选 number > 0 的车次
  │     → 解析 date ("2026/3/24") 为 LocalDate
  │     → 与目标日期列表取交集
  │
  ├─ 2. 对每个有票日期的 ScheduleItem:
  │     │
  │     ├─ 2a. OrderService.hasExistingOrder(routeId, scheduleId, date)
  │     │       → TqhApiClient.getRouteStops(routeId, [scheduleId])
  │     │       → 从响应中获取 route_name
  │     │       → TqhApiClient.getOrders("", 1, 10)
  │     │       → 遍历订单，匹配 route_name + date
  │     │       → 若 trade_state 为 "未支付"/"支付成功" → 跳过
  │     │
  │     ├─ 2b. OrderService.findUsableCoupons(routeId, scheduleId, boardingPointId)
  │     │       → TqhApiClient.getCoupons(...)
  │     │       → 筛选 is_use[scheduleId]=true, status[scheduleId]="待使用"
  │     │
  │     ├─ 2c. OrderService.tryVerifyCoupon(coupons, scheduleId)
  │     │       → 逐张调用 TqhApiClient.verifyPrice(...)
  │     │       → code=200 → 返回该优惠券
  │     │       → 全部失败 → 返回 empty
  │     │
  │     ├─ 2d. OrderService.placeOrder(scheduleId, coupon)
  │     │       → TqhApiClient.createOrder(...)
  │     │       → 返回 wx_order_id
  │     │
  │     └─ 2e. TicketLogService.logTicketPurchase(wxOrderId)
  │             → TqhApiClient.getOrders("", 1, 10)
  │             → 匹配 id == wxOrderId
  │             → 写入 ./logs/ticket.log
  │
  └─ 3. 等待 monitorInterval 秒 → 下一轮
```

---

## 6. 错误处理策略

| 场景 | 处理方式 | 说明 |
|------|---------|------|
| 第三方 API 网络超时 | 记录 WARN 日志，跳过本轮 | 下一轮自动重试 |
| API 返回非 200 code | 记录 WARN 日志，跳过当前步骤 | 不中断其他日期的处理 |
| Token 过期（认证失败） | 记录 ERROR 日志，提示用户更新 Token | 继续轮询（等待用户更新配置） |
| 优惠券验证失败 | 尝试下一张优惠券 | 全部失败则不使用优惠券 |
| 创建订单失败 | 记录 ERROR 日志，跳过该日期 | 下一轮会重新尝试 |
| 日志写入失败 | 记录 ERROR 日志 | 不影响下单流程 |

**实现方式**：在 `executeMonitorCycle` 和 `tryCreateOrder` 中使用 try-catch，确保单个日期的异常不影响其他日期。不使用全局异常处理器拦截业务异常——业务层自行处理并记录。

---

## 7. TDD 实施计划

按依赖关系从底层到上层逐步实施：

### Phase 1: 基础设施

| 顺序 | 类 | 关键测试用例 |
|------|-----|------------|
| 1 | `TqhProperties` | 配置属性绑定正确 |
| 2 | `RestClientConfig` | RestClient Bean 创建成功，请求头正确 |

### Phase 2: 外部 API 层

| 顺序 | 类 | 关键测试用例 |
|------|-----|------------|
| 3 | `TqhApiClient` | `should_parse_schedule_response_when_api_returns_success` |
| | | `should_parse_date_format_yyyy_slash_M_slash_d` |
| | | `should_return_coupon_list_when_api_returns_success` |
| | | `should_build_coupon_ids_correctly_for_price_verification` |
| | | `should_return_route_name_from_route_stops` |
| | | `should_create_order_and_return_wx_order_id` |
| | | `should_throw_when_api_returns_non_200_code` |

### Phase 3: 业务逻辑层

| 顺序 | 类 | 关键测试用例 |
|------|-----|------------|
| 4 | `DateRangeCalculator` | `should_return_wednesday_to_sunday_when_today_is_wednesday` |
| | | `should_return_next_monday_to_sunday` |
| | | `should_exclude_past_dates` |
| 5 | `OrderService` | `should_skip_date_when_existing_unpaid_order` |
| | | `should_skip_date_when_existing_paid_order` |
| | | `should_use_second_coupon_when_first_verification_fails` |
| | | `should_create_order_without_coupon_when_all_coupons_fail` |
| | | `should_filter_usable_coupons_by_is_use_and_status` |
| 6 | `TicketLogService` | `should_append_ticket_info_to_log_file` |
| | | `should_clear_log_file` |
| | | `should_format_log_with_separator` |
| 7 | `TicketMonitorService` | `should_intersect_target_dates_with_available_schedules` |
| | | `should_skip_dates_with_zero_tickets` |
| | | `should_continue_monitoring_when_api_fails` |

### Phase 4: 控制器层

| 顺序 | 类 | 关键测试用例 |
|------|-----|------------|
| 8 | `MonitorController` | `should_return_this_week_dates_when_post_this_week` |
| | | `should_return_next_week_dates_when_post_next_week` |
| | | `should_accept_custom_dates_when_post_dates` |
| | | `should_reject_invalid_date_format` |

---

## 8. 实施步骤

```
Step 1: 项目脚手架
        → Spring Boot 3.x 项目初始化（Maven）
        → application.yml 配置
        → TqhProperties + RestClientConfig

Step 2: 外部 API 层（TDD）
        → ScheduleItem / OrderItem / CouponItem 等模型
        → TqhApiClient + 测试

Step 3: 日期计算（TDD）
        → DateRangeCalculator + 测试

Step 4: 下单流程（TDD）
        → OrderService + 测试

Step 5: 日志服务（TDD）
        → TicketLogService + 测试

Step 6: 监控调度（TDD）
        → TicketMonitorService + 测试

Step 7: REST 端点（TDD）
        → MonitorController + 测试

Step 8: 集成验证
        → 端到端手动测试（curl 调用）
```
