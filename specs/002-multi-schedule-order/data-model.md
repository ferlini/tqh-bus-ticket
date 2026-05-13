# Phase 1 Data Model — 多车次合并下单与优惠券支持

**Feature**: 002-multi-schedule-order
**Date**: 2026-05-13

> 本特性**不**引入新的持久化实体；所有"实体"是内存中的领域对象，承载下单流程的输入/中间/输出状态。下文按 spec.md §Key Entities 给出的三个领域抽象 + 现有 HTTP DTO 的对应关系，构建可读、可测的数据结构。

---

## 实体 1：`ScheduleSelection`（车次选集）

**领域含义**：一次合并下单针对的 N 个有票车次，已经过去重、已发车过滤、已支付剔除。

**实现承载**：直接用 `List<ScheduleItem>`（既有 DTO），不创建新值对象。

**字段（来自既有 `ScheduleItem.java`）**：

| 字段 | 类型 | 说明 | 来源 |
|------|------|------|------|
| `id` | `int` | schedule_id，唯一标识一个车次 | 外部 `findSchedules` 接口 |
| `date` | `String` | 发车日期，格式 `yyyy/M/d`（如 `2026/3/24`） | 同上 |
| `time` | `String` | 发车时间，格式 `HH:mm`（如 `07:40`） | 同上 |
| `price` | `double` | 票价 | 同上 |
| `number` | `int` | 余票数；本特性使用前已过滤 `> 0` | 同上 |

**派生属性**（不存字段，按需计算）：

- `departureTime: LocalDateTime` = `LocalDate.parse(date, "yyyy/M/d") + LocalTime.parse(time, "HH:mm")`
- 排序键 = `departureTime`（升序）—— 对应 Earliest-date-first 算法

**不变式**：
1. 列表非空（空列表必须在入口拒绝）
2. `id` 在列表内唯一（重复必须在入口去重）
3. 所有 `departureTime` MUST 严格晚于当前时刻（已发车必须在入口过滤）

**生命周期**：
```
findSchedules() → 全量车次 Map
    ↓ filterAvailableSchedules（既有，按 number > 0 + 目标日期过滤）
    ↓ findPaidOrderDates（既有，剔除已支付）
    ↓ requireNonEmpty + deduplicate by id + filter not departed（新增，本特性）
ScheduleSelection                  ← 进入 OrderService.tryCreateMultiScheduleOrder 时的状态
```

---

## 实体 2：`CouponAssignment`（优惠券分配）

**领域含义**：从 `schedule_id` 到一张"已通过价格验证、未被同订单内其他车次消费"的 `CouponItem` 的映射。

**实现承载**：`Map<Integer, CouponItem>`（key = schedule_id，value = 该车次最终采用的优惠券）。**使用 `LinkedHashMap` 保留插入顺序**（按 `ScheduleSelection` 的日期升序），便于日志输出与测试断言。

**字段（既有 `CouponItem.java`）**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `int` | 优惠券实例 ID |
| `couponCategoryId` | `int` | 优惠券类别 ID（外部 API `coupon_ids` 嵌套结构所需） |
| `couponName` | `String` | 名称（仅用于日志，不参与决策） |
| `denomination` | `double` | 面额（仅用于日志，本特性不参与排序） |
| `isUse` | `Map<String, Boolean>` | 按 schedule_id 字符串映射的可用性 |
| `status` | `Map<String, String>` | 按 schedule_id 字符串映射的状态（仅 `"待使用"` 视为可分配） |

**不变式**：
1. **唯一性**：`map.values()` 中 `CouponItem.id` 必须互不相同（同张券不可分配给两个车次）
2. **稀疏性**：`map.size() ≤ ScheduleSelection.size()`（每个车次至多一张券）；缺失键 = 该车次无券（合法）
3. **可用性**：对每个 `(scheduleId, coupon)` 对，`coupon.isUse.get(String.valueOf(scheduleId)) == true` 且 `coupon.status.get(String.valueOf(scheduleId)) == "待使用"`
4. **已验证**：每个 entry MUST 经过 `verifyPrice` 调用并通过

**生命周期**：
```
findUsableCouponsForSchedules(routeId, allScheduleIds, boardingPointId)
    → List<CouponItem>（全量，未按车次拆分）
    ↓ allocateCoupons(schedulesAscByDate, allCoupons)
    ↓   ┌─ for each schedule:
    ↓   │     usable = allCoupons filter (isUsable(c, scheduleStr) && !consumed.contains(c.id))
    ↓   │     verified = tryVerifyCoupon(usable, scheduleId)
    ↓   │     if present: assignment.put(...); consumed.add(verified.id)
    ↓   └─
CouponAssignment（empty allowed）   ← 进入 placeMultiScheduleOrder 时的状态
```

---

## 实体 3：`MultiScheduleOrder`（多车次订单）

**领域含义**：合并下单的最终请求与响应抽象。

**实现承载**：直接复用既有 `CreateOrderRequest` / `CreateOrderResponse`，不创建新类。

### 请求侧 — `CreateOrderRequest`（既有）

| 字段 | 类型 | 多车次场景下的含义 |
|------|------|---------------------|
| `routeId` | `int` | 来自 `TqhProperties.routeId`（路线 ID） |
| `boardingPointId` | `int` | 来自 `TqhProperties.boardingPointId` |
| `alightingPointId` | `int` | 来自 `TqhProperties.alightingPointId` |
| `scheduleIds` | `List<Integer>` | 来自 `ScheduleSelection` 提取的所有 `schedule.id` |
| `couponIds` | `Map<String, Map<String, Integer>>` | 由 `CouponAssignment` 转换得到（见下方"转换规则"） |

#### 转换规则：`CouponAssignment` → `CreateOrderRequest.couponIds`

```text
for each (scheduleId, coupon) in assignment:
    outer key = String.valueOf(scheduleId)
    inner key = String.valueOf(coupon.couponCategoryId)
    inner value = coupon.id
=> {outer: {inner: value}}

空 assignment → 空 Map（不是 null）
```

#### 示例（N=2，两张不同优惠券）

```json
{
  "route_id": 275,
  "boarding_point_id": 24,
  "alighting_point_id": 400,
  "schedule_ids": [61429, 61512],
  "coupon_ids": {
    "61429": {"2": 8317178},
    "61512": {"2": 8190582}
  }
}
```

#### 示例（N=2，仅一个车次有券）

```json
{
  "route_id": 275,
  "boarding_point_id": 24,
  "alighting_point_id": 400,
  "schedule_ids": [61429, 61512],
  "coupon_ids": {
    "61429": {"2": 8317178}
  }
}
```

#### 示例（N=2，全部无券）

```json
{
  "route_id": 275,
  "boarding_point_id": 24,
  "alighting_point_id": 400,
  "schedule_ids": [61429, 61512],
  "coupon_ids": {}
}
```

### 响应侧 — `CreateOrderResponse`（既有）

| 字段 | 类型 | 多车次场景下的含义 |
|------|------|---------------------|
| `wxOrderId` | `int` | 微信侧的订单 ID，唯一标识本次合并订单 |
| （其他字段保持既有） | | 无新增需求 |

---

## 状态转换

```
                        ┌─────────────────────┐
                        │ 监控 cycle 开始       │
                        └──────────┬──────────┘
                                   │
                  findSchedules + filter（既有）
                                   │
                                   ▼
                      ┌────────────────────────┐
                      │ availableSchedules     │
                      │ (List<ScheduleItem>)    │
                      └──────────┬─────────────┘
                                  │
                  findPaidOrderDates 剔除已支付（既有）
                                   │
            ┌──────────────────────┴────────────────────────┐
            │（待支付 → 抛 UnpaidOrderException，流程中止）  │
            └──────────────────────┬────────────────────────┘
                                   │
                  入口校验：非空 + 去重 + 已发车过滤
                                   │
                                   ▼
                      ┌────────────────────────┐
                      │ ScheduleSelection       │  ← 不变式建立
                      └──────────┬─────────────┘
                                  │
                  findUsableCouponsForSchedules
                                   │
                                   ▼
                      ┌────────────────────────┐
                      │ List<CouponItem>        │  ← 全量候选
                      └──────────┬─────────────┘
                                  │
                  allocateCoupons (Earliest-date-first)
                                   │
                                   ▼
                      ┌────────────────────────┐
                      │ CouponAssignment        │  ← 唯一性 + 稀疏性 + 已验证
                      └──────────┬─────────────┘
                                  │
                  placeMultiScheduleOrder
                                   │
                                   ▼
                      ┌────────────────────────┐
                      │ CreateOrderResponse     │
                      │ (wxOrderId)             │
                      └──────────┬─────────────┘
                                  │
                  TicketLogService.logTicketPurchase
                  + OpenClawWebhookClient.notifyTicketPurchase
                                   │
                                   ▼
                              监控 cycle 结束
```

---

## 与既有数据模型的关系

本特性**不修改**任何既有 DTO（`CreateOrderRequest`、`CouponRequest`、`PriceVerificationRequest`、`CouponItem`、`ScheduleItem`）的字段或 JSON 结构——它们在 001-monitor-and-buy-ticket 阶段就已具备 `List<Integer> scheduleIds` 与 nested map `couponIds` 的多车次能力，本特性只是**真正用上**这些字段的多元素形态。
