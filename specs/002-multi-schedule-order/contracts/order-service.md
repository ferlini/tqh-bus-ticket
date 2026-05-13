# Contract — `OrderService` 多车次方法

**Feature**: 002-multi-schedule-order
**Date**: 2026-05-13
**Scope**: 本项目纯属内部 Java 服务，无对外公开 REST/RPC 契约改动。该契约文档记录 **`service` 层 Java 方法签名 + 行为契约**，作为单元测试的输入。

---

## 1. 新增方法

### 1.1 `tryCreateMultiScheduleOrder`

```java
public boolean tryCreateMultiScheduleOrder(List<ScheduleItem> schedules);
boolean tryCreateMultiScheduleOrder(List<ScheduleItem> schedules, LocalDateTime now);  // 包私有；测试用
```

**前置条件 (Pre)**
- `schedules` 非 `null`；非空（空 list MUST 抛 `BusinessException`）
- 列表元素的 `id` MUST 是有效 schedule_id；列表内允许重复，由本方法去重
- `now` MUST NOT 为 `null`（仅在测试入口）

**后置条件 (Post)**
- 返回 `true`：恰好向 `TqhApiClient.createOrder` 发送了 **1 次** 调用且该调用成功；`lastCreatedOrderId` 已更新
- 返回 `false`：经过去重 + 已发车过滤后，schedule 列表为空（无可下单车次），未调用 `createOrder`
- 抛 `BusinessException`：`schedules` 为空 或 `null`
- 抛 `UnpaidOrderException`：本方法**不**抛——该过滤由调用方（`TicketMonitorService`）在调用前完成（沿用 001 阶段设计）

**不变式 (Invariants)**
- 单车次（`schedules.size() == 1` 且去重过滤后仍为 1）下，行为与既有 `tryCreateOrder(ScheduleItem, LocalDateTime)` **完全等价**（同样的 `createOrder` 报文、同样的 `lastCreatedOrderId` 赋值、同样的日志输出）—— 满足 FR-010。
- 全部已发车 → 返回 `false`，**不**调用 `findUsableCouponsForSchedules` / `allocateCoupons` / `placeMultiScheduleOrder` / `createOrder`。

**外部 API 调用图（成功路径，N=2，两张不同券）**
```
findUsableCouponsForSchedules(routeId, [s1.id, s2.id], boardingPointId)  → 1 次 getCoupons
allocateCoupons(schedules sorted asc, allCoupons)
    → tryVerifyCoupon(usable for s1, s1.id)    → ≥1 次 verifyPrice（直到通过或全失败）
    → tryVerifyCoupon(usable for s2, s2.id)    → ≥1 次 verifyPrice（同上）
placeMultiScheduleOrder([s1.id, s2.id], assignment)  → 1 次 createOrder
```

---

### 1.2 `findUsableCouponsForSchedules`

```java
List<CouponItem> findUsableCouponsForSchedules(int routeId, List<Integer> scheduleIds, int boardingPointId);  // 包私有
```

**Pre**
- `routeId > 0`、`boardingPointId > 0`
- `scheduleIds` 非空，元素去重已由 caller 保证

**Post**
- 返回值 = `TqhApiClient.getCoupons(routeId, scheduleIds, boardingPointId)` 的全部 `CouponItem`，**不**在此层按车次过滤可用性（由 `allocateCoupons` 现地判断）
- 恰好 1 次 `getCoupons` 调用

---

### 1.3 `allocateCoupons`

```java
Map<Integer, CouponItem> allocateCoupons(List<ScheduleItem> schedules, List<CouponItem> allCoupons);  // private
```

**Pre**
- `schedules` 非空、`id` 唯一、未发车
- `allCoupons` 可为空列表（不为 `null`）

**Post**
- 返回 `LinkedHashMap<Integer, CouponItem>`
- key 顺序 = `schedules` 按 `(date, time)` 升序后的 `id` 序列（仅包含成功分配到券的 schedule）
- value 之间 `CouponItem.id` 两两不等（同一张券不会分配给两个 schedule）—— 不变式：同订单内无券复用
- 缺失 key = 该 schedule 没有可用券（或可用券全验证失败）

**Earliest-date-first 算法行为（来自 Clarifications #1）**
```
sorted = schedules sorted by parse(date) + parse(time) asc
consumed = {}
out = {}
for s in sorted:
    candidates = allCoupons.filter(c ->
        Boolean.TRUE.equals(c.isUse.get(str(s.id))) &&
        "待使用".equals(c.status.get(str(s.id))) &&
        !consumed.contains(c.id))
    verified = tryVerifyCoupon(candidates, s.id)
    if verified.isPresent():
        out.put(s.id, verified.get())
        consumed.add(verified.get().id)
return out
```

---

### 1.4 `placeMultiScheduleOrder`

```java
CreateOrderResponse placeMultiScheduleOrder(List<Integer> scheduleIds, Map<Integer, CouponItem> assignment);  // 包私有
```

**Pre**
- `scheduleIds` 非空、元素已去重、对应的车次未发车
- `assignment` 非 `null`（可为空 map）；key 集合 MUST 是 `scheduleIds` 的子集；value 间 id 唯一

**Post**
- 恰好 1 次 `TqhApiClient.createOrder(CreateOrderRequest)` 调用
- 请求体满足：
  - `routeId` = `properties.getRouteId()`
  - `boardingPointId` = `properties.getBoardingPointId()`
  - `alightingPointId` = `properties.getAlightingPointId()`
  - `scheduleIds` 等于入参（保持顺序）
  - `couponIds` 对每个 entry `(sId, coupon)` in assignment 产生 `{String.valueOf(sId): {String.valueOf(coupon.couponCategoryId): coupon.id}}`；空 assignment → 空 map（不是 `null`）

---

## 2. 修改方法（保留签名 + 行为）

### 2.1 `tryCreateOrder(ScheduleItem, LocalDateTime)`

**新实现**：委托到 `tryCreateMultiScheduleOrder(List.of(schedule), now)`。
**行为**：与既有完全一致（FR-010）。所有现有 `OrderServiceTest` 用例（`should_return_false_when_departure_time_has_passed`、`should_return_true_when_order_created_successfully`、`should_return_true_when_all_coupons_fail_and_order_without_coupon`、`should_proceed_when_departure_time_not_passed`）MUST 在不修改的情况下通过。

### 2.2 `findUsableCoupons(int routeId, int scheduleId, int boardingPointId)`

**新实现**：委托到 `findUsableCouponsForSchedules(routeId, List.of(scheduleId), boardingPointId)` + 按 scheduleId 过滤可用性。
**行为**：返回结构、可用性判定、对外效应（恰好 1 次 `getCoupons` 调用）与既有一致。

### 2.3 `placeOrder(int scheduleId, Optional<CouponItem> coupon)`

**新实现**：委托到 `placeMultiScheduleOrder(List.of(scheduleId), coupon.map(c -> Map.of(scheduleId, c)).orElse(Map.of()))`。
**行为**：与既有完全一致。

### 2.4 `findPaidOrderDates`、`tryVerifyCoupon`

**不修改**。

---

## 3. `TicketMonitorService` 侧的契约影响

### 3.1 移除方法

```java
private void processSchedule(ScheduleItem schedule)
```

### 3.2 新增方法

```java
private void processSchedules(List<ScheduleItem> schedules);
```

**行为**：
- 调用 `orderService.tryCreateMultiScheduleOrder(schedules)`
- 若返回 `true`：`ticketLogService.logTicketPurchase(orderService.getLastCreatedOrderId())` + `openClawWebhookClient.notifyTicketPurchase(message)`，恰好各 1 次
- `UnpaidOrderException` 仍向上抛（由 `executeMonitorCycle` 处理）
- 其他异常 catch + log + 不中断 cycle（与既有 `processSchedule` 一致——但因为本轮只有一个"逻辑单元"，下一轮整体重试）

### 3.3 调用点修改

`executeMonitorCycle` 中：
```diff
- for (ScheduleItem schedule : availableSchedules) {
-     processSchedule(schedule);
- }
+ processSchedules(availableSchedules);
```

---

## 4. 测试断言要点（供 /speckit-tasks 拆任务时引用）

| 测试名（建议） | 覆盖契约 |
|---------------|---------|
| `should_throw_when_multi_schedule_list_is_empty` | 1.1 Pre |
| `should_deduplicate_schedules_before_submission` | 1.1 Pre |
| `should_skip_when_all_schedules_already_departed` | 1.1 Post / 不变式 |
| `should_create_single_order_for_two_schedules_with_distinct_coupons` | 1.1 Post + 1.4 Post |
| `should_call_get_coupons_once_for_all_schedules` | 1.2 Post |
| `should_allocate_shared_coupon_to_earliest_schedule` | 1.3 算法行为 |
| `should_leave_later_schedule_without_coupon_when_only_one_shared_coupon` | 1.3 算法行为 |
| `should_keep_n_equals_1_behavior_identical_to_single_schedule` | FR-010 / 2.1 / 2.2 / 2.3 |
| `should_build_create_order_request_with_empty_coupon_ids_when_no_coupons_verified` | 1.4 Post |
| `should_serialize_multi_schedule_create_order_request_correctly` | 集成测试（`TqhApiClientTest`） |
| `should_invoke_create_order_once_when_monitor_finds_two_available_schedules` | 3.2 Post（`TicketMonitorServiceTest`） |
| `should_send_single_webhook_for_merged_order` | 3.2 Post |
