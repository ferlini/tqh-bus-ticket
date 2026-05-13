# Phase 0 Research — 多车次合并下单与优惠券支持

**Feature**: 002-multi-schedule-order
**Date**: 2026-05-13
**Purpose**: 把 plan.md Technical Context 中所有非显然的技术决策固化下来，并解释为何拒绝其他选项。`spec.md` 的 Clarifications 与 Assumptions 已经回答了所有业务侧问题；本文件只覆盖**技术实现侧**的决策。

---

## Decision 1 — `OrderService` 的多车次方法签名

**Decision**: 在 `OrderService` 上新增两个公开方法：
```java
boolean tryCreateMultiScheduleOrder(List<ScheduleItem> schedules);
boolean tryCreateMultiScheduleOrder(List<ScheduleItem> schedules, LocalDateTime now);  // 包私有，仅供测试注入时钟
```
现有的 `tryCreateOrder(ScheduleItem)` / `tryCreateOrder(ScheduleItem, LocalDateTime)` **保留**，内部委托到多车次版本（`tryCreateMultiScheduleOrder(List.of(schedule), now)`）。

**Rationale**:
- 满足 FR-010："N=1 行为与既有完全等价" —— 委托即可保证 1:1 的语义保留，所有现有 `OrderServiceTest` 用例无需修改即应通过。
- 双签名（无 now / 有 now）沿用现有 `tryCreateOrder` 模式，时钟注入用于测试，无需引入 `Clock` Spring bean（简单性原则）。
- 不引入新的 `MultiScheduleOrderService` 类——只有一个使用点（`TicketMonitorService`），独立类是过度抽象。

**Alternatives considered**:
- **替换单车次方法**：删除 `tryCreateOrder(ScheduleItem)`，强制所有调用方迁移到 List 版本。拒绝原因：违反"对内向兼容"以及现有测试需要全部改动，增加回归风险，与 FR-010 表述精神（"零差异"）冲突。
- **新增独立类 `MultiScheduleOrderService`**：拒绝原因：仅一个 caller，重复 `OrderService` 持有的 `apiClient` / `properties` 依赖；单一职责并未被违反——下单流程编排仍是同一职责。

---

## Decision 2 — 优惠券分配算法

**Decision**: 实现为 `OrderService` 内的 private 方法 `allocateCoupons(List<ScheduleItem> sortedByDate, List<CouponItem> allCoupons): Map<Integer, CouponItem>`，伪代码：

```
schedulesAscByDate = schedules sorted by parseDate(schedule.date) + LocalTime.parse(schedule.time)
consumedCouponIds = new HashSet<Integer>()
assignment = new LinkedHashMap<Integer, CouponItem>()

for each schedule in schedulesAscByDate:
    usable = allCoupons.filter(c -> isUsableCoupon(c, scheduleIdStr(schedule))
                                   && !consumedCouponIds.contains(c.id))
    verified = tryVerifyCoupon(usable, schedule.id)   // 复用既有方法
    if verified.isPresent():
        assignment.put(schedule.id, verified.get())
        consumedCouponIds.add(verified.get().id)
return assignment
```

**Rationale**:
- 完全实现 Clarifications #1 的 Earliest-date-first：按 (date, time) 升序处理，先到的车次先消费可选券。
- 复用现有 `tryVerifyCoupon(coupons, scheduleId)`——验证逻辑保持 per-(schedule, coupon) 逐张试错语义（Assumption "批量验证退化为逐次验证"）。
- `LinkedHashMap` 保留插入顺序，便于日志输出与测试断言。
- 圈复杂度：单 for 循环 + 单 if = 3，远低于 ≤10 的宪法上限。

**Alternatives considered**:
- **Scarcity-first**：先服务"可用券最少"的车次。已在 /speckit-clarify Q1 被显式否决（用户选 A）。
- **批量 verify（一次性传所有 (schedule, coupon) 候选）**：会改变 verifyPrice 调用语义并要求实现"哪一对失败"的归因逻辑，与 Assumption 冲突。

---

## Decision 3 — `findUsableCoupons` 扩展为 `findUsableCouponsForSchedules`

**Decision**: 新增公开方法
```java
List<CouponItem> findUsableCouponsForSchedules(int routeId, List<Integer> scheduleIds, int boardingPointId);
```
返回**所有**车次共同从外部 API 拉到的优惠券列表（不在此层做按车次拆分；按车次的可用性由 `allocateCoupons` 内的 `isUsableCoupon(coupon, scheduleIdStr)` 现地判断）。现有 `findUsableCoupons(routeId, scheduleId, boardingPointId)` 保留，内部委托到新方法 + 现地过滤。

**Rationale**:
- 外部 `TqhApiClient.getCoupons(routeId, scheduleIds, boardingPointId)` 已支持多 schedule_id（DTO `CouponRequest.scheduleIds: List<Integer>`），一次调用返回的 `CouponItem` 自带 `isUse: Map<scheduleIdStr, Boolean>` 与 `status: Map<scheduleIdStr, String>`，天然按车次组织。
- 把"按车次过滤"放在分配阶段而非查询阶段，避免重复构造多份过滤后的 List；同时让"优惠券是否对某车次可用"的语义集中在 `isUsableCoupon(coupon, scheduleIdStr)` 一处。
- 单一 HTTP 调用承担全部车次的优惠券查询，减少 N 次 `getCoupons` 调用的浪费——直接实现 SC-001 的精神（合并请求次数）。

**Alternatives considered**:
- **保留 N 次 `findUsableCoupons` 调用，每次传 `List.of(scheduleId)`**：拒绝原因：相同 routeId / boardingPointId 下的 N 次调用会返回大量重复数据，且外部 API 已经允许传 List。
- **新方法返回 `Map<Integer, List<CouponItem>>`（按 schedule_id 预过滤）**：拒绝原因：信息冗余（同一张可对多车次都可用的券会被复制到多个 list），且使 `allocateCoupons` 内的"全局已消费集合"难以维护。

---

## Decision 4 — `placeOrder` 扩展为 `placeMultiScheduleOrder`

**Decision**: 新增公开方法
```java
CreateOrderResponse placeMultiScheduleOrder(List<Integer> scheduleIds, Map<Integer, CouponItem> assignment);
```
现有 `placeOrder(int scheduleId, Optional<CouponItem> coupon)` 保留，内部委托：
```java
placeMultiScheduleOrder(List.of(scheduleId),
                       coupon.map(c -> Map.of(scheduleId, c)).orElse(Map.of()));
```

**Rationale**:
- `CreateOrderRequest.scheduleIds: List<Integer>` 与 `couponIds: Map<scheduleIdStr, Map<categoryIdStr, couponId>>` 已是多车次结构。仅需在新方法中把 `Map<Integer, CouponItem>` 转换为外部 API 要求的 nested map。
- 委托保证 FR-010 的 N=1 路径零差异。
- 转换逻辑（`Map<Integer, CouponItem>` → nested map）极小（≤ 5 行），保持单一职责，不需要单独类。

**Alternatives considered**:
- **删除单车次 `placeOrder`，强制多车次签名**：拒绝原因：现有 OrderServiceTest 中 `should_create_order_with_coupon` / `should_create_order_without_coupon` 用例就会必须改动，违反 FR-010 的"零回归"。

---

## Decision 5 — `TicketMonitorService.processSchedule` → `processSchedules`

**Decision**: 把 `executeMonitorCycle` 中的循环：
```java
for (ScheduleItem schedule : availableSchedules) { processSchedule(schedule); }
```
替换为单次合并调用：
```java
processSchedules(availableSchedules);
```
其中 `processSchedules(List<ScheduleItem>)`：(1) 调用 `orderService.tryCreateMultiScheduleOrder(schedules)`；(2) 若返回 true，记日志 + webhook 通知；(3) 通过 `UnpaidOrderException` 仍然向上抛出（与既有 `processSchedule` 行为一致）。

**Rationale**:
- 直接实现 US1 AC1：N 个有票车次 → 1 个订单 → 1 条日志 + 1 个 webhook（FR-006 / SC-001 / SC-003）。
- 既有"单 schedule 失败不影响其他 schedule"的隔离需求消失——因为现在 N 个 schedule 是一个原子单元（同一个外部订单）。失败时本轮跳过，下一轮重试，与 Assumption "重试与失败处理" 一致。

**Alternatives considered**:
- **保留 per-schedule 循环，每次只跑单车次 `tryCreateOrder`**：直接违反 US1（不会合并下单），且 SC-001 / SC-003 失败。
- **批量分块（每 K 个 schedule 一组）**：拒绝原因：N 的实际上界是 ≤ 7，分块毫无必要；引入分块策略 = 不必要复杂度，违反简单性原则。

---

## Decision 6 — `findPaidOrderDates` 的 schedule_id 参数

**现状**: `findPaidOrderDates(int routeId, int scheduleId, Set<LocalDate> targetDates)` 接受单个 schedule_id，仅用于 `getRouteStops(routeId, List.of(scheduleId))` 取 `route_name`。在多车次场景下，`route_name` 与具体 schedule_id 无关（同一路线下所有车次共享同一 route_name），所以传任意一个 schedule_id 都能拿到正确的 route_name。

**Decision**: **不修改 `findPaidOrderDates` 签名**。在 `TicketMonitorService.executeMonitorCycle` 中继续传 `availableSchedules.get(0).getId()` 作为代表 schedule_id（既有代码已是此模式，第 135 行）。

**Rationale**:
- 该参数本质是"任意一个该 route 下的 schedule_id"，并非"要查的目标 schedule"——签名虽不优雅但行为正确，且既有测试 `should_return_paid_dates_matching_route_and_target_dates` 已覆盖。
- 改造该方法签名会引入与本特性无关的回归面，违反"修改最小"原则。

**Alternatives considered**:
- **重命名参数为 `representativeScheduleId` 或新增重载方法接受 `List<Integer>`**：可读性确有微小提升，但不是 FR 的一部分；本特性聚焦"合并下单"，纯命名优化推迟到后续 PR。

---

## Decision 7 — Edge case 实现位置

| 边界情况 | 实现位置 |
|---------|---------|
| 空 `schedule_ids` → 抛业务异常 | `tryCreateMultiScheduleOrder` 方法首行（`Objects.requireNonNull` + `if (schedules.isEmpty()) throw new BusinessException(...)`） |
| 重复 `schedule_id` 去重 | `tryCreateMultiScheduleOrder` 开头：`schedules.stream().collect(Collectors.toMap(ScheduleItem::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new)).values()` |
| 车次已发车过滤 | `tryCreateMultiScheduleOrder` 内部过滤：`filter(s -> now.isBefore(departureTimeOf(s)))`；过滤后空则返回 `false`（与现行单车次"已发车跳过"行为对齐） |
| 同券冲突按日期升序 | `allocateCoupons` 内排序（Decision 2） |
| 优惠券验证全部失败 | `allocateCoupons` 自然落到"该 schedule 无 entry"，下单时此 schedule 不出现在 `couponIds`，订单仍提交 |
| 待支付订单存在 | `executeMonitorCycle` 现有的 `orderService.findPaidOrderDates` 抛 `UnpaidOrderException`；流程在 `processSchedules` 之前就已 abort，无需在 `OrderService` 二次处理 |

**Rationale**: 所有边界处理集中在最早能判断的层级（输入校验 + 过滤在入口，分配规则在 `allocateCoupons`），不重复实现，便于读懂。

---

## Decision 8 — 测试金字塔与覆盖策略

**Decision**:
- **单元测试**（主体，~10 个新用例 in `OrderServiceTest`）：mock `TqhApiClient` 与 `TqhProperties`，覆盖：
  - 多车次端到端成功 / 部分券 / 全无券 / 一张共享券分配 / 重复 schedule_id 去重 / 空列表抛错 / N=1 退化 / 已发车过滤 / `placeMultiScheduleOrder` 报文结构 / `findUsableCouponsForSchedules` 单调用合并查询
- **集成测试**（1 个新用例 in `TqhApiClientTest`）：用 `MockRestServiceServer` 断言 `createOrder` 在 N=2 时实际 POST 报文的 JSON 结构（`schedule_ids: [..., ...]` + `coupon_ids: {"id1": {...}, "id2": {...}}`），保证 DTO 序列化在多车次下不退化。
- **TicketMonitorService 单元测试**（~2 个新用例）：mock `OrderService.tryCreateMultiScheduleOrder`，断言 (a) N=2 时只调用一次；(b) 成功时仅一次 webhook + 一次日志。

**Rationale**:
- 单元测试是主体（宪法 §测试金字塔）；集成测试只在 HTTP 序列化边界出现新风险时新增。
- 不写端到端 HTTP→DB→HTTP 测试——本项目无 DB、无外部 sandbox，端到端验证由 quickstart.md 的人工 curl 完成。

---

## 风险与回归点

| 风险 | 概率 | 影响 | 缓解 |
|------|------|------|------|
| `placeMultiScheduleOrder` 报文 JSON 结构与外部 API 期望不一致 | 中 | 高（订单创建失败） | 新增集成测试验证报文；首次部署后用真实账号做一次 N=2 验证（quickstart.md 步骤） |
| 优惠券分配算法在"同券对 N 个车次都可用"时的实际外部接受情况 | 中 | 中（验证失败回退到无券，订单仍创建） | `tryVerifyCoupon` 现有失败-继续语义会兜底 |
| `findUsableCouponsForSchedules` 返回的 `CouponItem.isUse` 在 schedule_id 不在 `scheduleIds` 列表中时是否有键 | 低 | 低（仅影响一致性检查） | 由 `isUsableCoupon` 的 `Boolean.TRUE.equals(canUse)` 做空安全防御 |
| FR-010 回归：N=1 路径行为差异 | 低 | 高 | 保留所有现有 `OrderServiceTest` 用例不修改即应通过；CI 失败立刻能发现 |

---

## 所有 NEEDS CLARIFICATION 状态

✅ Technical Context 中无 NEEDS CLARIFICATION 残留——所有技术决策已在上文固化。
