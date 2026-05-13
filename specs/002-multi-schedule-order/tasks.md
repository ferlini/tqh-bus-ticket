---
description: "Task list for 002-multi-schedule-order"
---

# Tasks: 多车次合并下单与优惠券支持

**Input**: Design documents from `/specs/002-multi-schedule-order/`
**Prerequisites**: spec.md, plan.md, research.md, data-model.md, contracts/order-service.md, quickstart.md
**Constitution gate**: 测试先行 (Test-First) NON-NEGOTIABLE — 每个实现任务必须有先行的失败测试任务

**Organization**: Tasks are grouped by user story (US1/US2/US3) to enable independent verification of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: 标记任务归属的用户故事（US1/US2/US3）；Setup/Foundational/Polish 阶段任务**不**带 Story 标签
- Each implementation task includes the exact file path

## Path Conventions

Single-module Maven project (沿用既有结构):
- 生产代码：`src/main/java/com/tqh/bus/ticket/...`
- 测试代码：`src/test/java/com/tqh/bus/ticket/...`

---

## Phase 1: Setup (Baseline)

**Purpose**: 确认当前主线绿灯，为 TDD 循环建立可对比的基准。

- [x] T001 在 `feature/002-multi-schedule-order` 分支根目录运行 `mvn -q clean test` 并记录初始通过用例数与耗时；任何失败 MUST 在进入 Phase 2 前修复或还原本地变更
- [x] T002 用 `git log --oneline -5` 与 `git status` 截图（或纯文本记录）保留 baseline 提交 `0369e4e` 的引用，便于后续回滚定位

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 建立多车次下单入口的最小骨架——入口校验 + 去重 + 已发车过滤。所有三个用户故事都依赖该骨架，必须先完成。

**⚠️ CRITICAL**: 在 T006 完成前，US1/US2/US3 的任务无法开始。

- [x] T003 在 `src/test/java/com/tqh/bus/ticket/service/OrderServiceTest.java` 末尾新增三个失败测试：`should_throw_business_exception_when_multi_schedule_list_is_empty`、`should_deduplicate_schedules_before_submitting_multi_order`、`should_skip_when_all_schedules_already_departed_in_multi_order`；mock `apiClient` 并断言无任何 `createOrder` 调用
- [x] T004 在 `src/main/java/com/tqh/bus/ticket/service/OrderService.java` 中新增公开方法 `boolean tryCreateMultiScheduleOrder(List<ScheduleItem> schedules)` + 包私有重载 `tryCreateMultiScheduleOrder(List<ScheduleItem>, LocalDateTime now)`；当前只实现：(a) null/empty → 抛 `BusinessException`；(b) 按 id 去重保留首次出现；(c) 过滤掉 `departureTime <= now` 的车次；(d) 过滤后为空 → 返回 `false`；过滤后非空 → 暂时抛 `UnsupportedOperationException("Phase 2 skeleton — not yet implemented")`，留给 US1 完成
- [x] T005 运行 `mvn -q test -Dtest=OrderServiceTest`，确认 T003 的三个测试 GREEN；现有 OrderServiceTest 用例 MUST 全部保持 GREEN（说明 N=1 的既有 `tryCreateOrder` 路径未被破坏，因为还没改它）

**Checkpoint**: Phase 2 完成后，`tryCreateMultiScheduleOrder` 入口存在但 happy-path 抛 `UnsupportedOperationException`；这是预期状态，由 US1 接续实现。

---

## Phase 3: User Story 1 — 同一监控轮次内多个可购车次合并为一个订单 (Priority: P1) 🎯 MVP

**Goal**: 监控发现 N≥1 个有票车次时，系统调用 1 次 `TqhApiClient.createOrder` 合并下单；N=1 的行为与既有完全一致（FR-010）；下单成功后产生 1 条日志 + 1 次 webhook（FR-006）。

**Independent Test**: mock `TqhApiClient`，调用 `orderService.tryCreateMultiScheduleOrder(List.of(s1, s2))` 后断言：`createOrder` 被调用恰好 1 次、请求体的 `scheduleIds` 等于 `[s1.id, s2.id]`、返回 `true`；并通过 `MockRestServiceServer` 验证实际 HTTP 报文的 JSON 结构正确。

### Tests for User Story 1 (TDD — MUST FAIL before implementation) ⚠️

- [x] T006 [P] [US1] 在 `src/test/java/com/tqh/bus/ticket/integration/TqhApiClientTest.java` 新增集成测试 `should_serialize_multi_schedule_create_order_request_correctly`：用 `MockRestServiceServer` 拦截 `/api/v2/order/create`，断言请求体 JSON 中 `schedule_ids: [61429, 61512]` 与 `coupon_ids: {}`（空 map，非 `null`）的结构与字段序列化正确
- [x] T007 [US1] 在 `src/test/java/com/tqh/bus/ticket/service/OrderServiceTest.java` 新增失败测试 `should_create_single_order_for_two_schedules_with_no_coupons`：两个未发车 schedule、`getCoupons` 返回空、断言 `createOrder` 被调用 1 次且请求体的 `scheduleIds.size() == 2`、`couponIds.isEmpty()`、方法返回 `true`、`lastCreatedOrderId` 被赋值
- [x] T008 [P] [US1] 在 `src/test/java/com/tqh/bus/ticket/service/TicketMonitorServiceTest.java` 新增失败测试 `should_invoke_create_multi_schedule_order_once_when_two_available_schedules`：两个有票车次进入 `executeMonitorCycle`，断言 `orderService.tryCreateMultiScheduleOrder` 被调用恰好 1 次且参数为 `List.of(s1, s2)`，`tryCreateOrder(ScheduleItem)` 不被调用
- [x] T009 [US1] 在 `src/test/java/com/tqh/bus/ticket/service/TicketMonitorServiceTest.java` 新增失败测试 `should_send_single_webhook_and_log_when_merged_order_succeeds`：mock `tryCreateMultiScheduleOrder` 返回 `true`，断言 `ticketLogService.logTicketPurchase` 调用 1 次、`openClawWebhookClient.notifyTicketPurchase` 调用 1 次（FR-006）
- [x] T010 [US1] 在 `src/test/java/com/tqh/bus/ticket/service/OrderServiceTest.java` 新增 N=1 回归断言 `should_keep_n_equals_1_behavior_identical_when_called_via_multi_schedule_entry`：用 `List.of(s1)` 调用 `tryCreateMultiScheduleOrder`，验证产生的 `createOrder` 请求体与既有 `tryCreateOrder(s1)` 产生的请求体逐字段相等

### Implementation for User Story 1 (after all US1 tests fail) ⚠️

- [x] T011 [US1] 在 `src/main/java/com/tqh/bus/ticket/service/OrderService.java` 新增包私有方法 `CreateOrderResponse placeMultiScheduleOrder(List<Integer> scheduleIds, Map<Integer, CouponItem> assignment)`：构造 `CreateOrderRequest`，按 contracts/order-service.md §1.4 的转换规则把 `assignment` 转为 `Map<String, Map<String, Integer>>`；空 assignment → 空 map（非 `null`）；调用 `apiClient.createOrder` 并返回响应
- [x] T012 [US1] 在 `src/main/java/com/tqh/bus/ticket/service/OrderService.java` 完成 `tryCreateMultiScheduleOrder(List<ScheduleItem>, LocalDateTime)` 的 happy-path 主体：在已过滤的 schedule 列表上调用 `placeMultiScheduleOrder(scheduleIds, Map.of())`（暂用空 assignment——优惠券分配将在 US2 接入），把响应的 `wxOrderId` 写入 `lastCreatedOrderId`，记录 `log.info` 含全部 schedule 日期，返回 `true`；移除 Phase 2 留下的 `UnsupportedOperationException`
- [x] T013 [US1] 在 `src/main/java/com/tqh/bus/ticket/service/OrderService.java` 把现有 `tryCreateOrder(ScheduleItem)` 与 `tryCreateOrder(ScheduleItem, LocalDateTime)` 改写为对 `tryCreateMultiScheduleOrder(List.of(schedule), now)` 的委托；保留方法签名（FR-010 + Decision 1 of research.md）
- [x] T014 [US1] 在 `src/main/java/com/tqh/bus/ticket/service/OrderService.java` 把现有 `placeOrder(int scheduleId, Optional<CouponItem> coupon)` 改写为对 `placeMultiScheduleOrder(List.of(scheduleId), coupon.map(c -> Map.of(scheduleId, c)).orElse(Map.of()))` 的委托；保留方法签名
- [x] T015 [US1] 在 `src/main/java/com/tqh/bus/ticket/service/TicketMonitorService.java` 把 `executeMonitorCycle` 中 `for (ScheduleItem schedule : availableSchedules) { processSchedule(schedule); }` 替换为 `processSchedules(availableSchedules)`；新增私有方法 `processSchedules(List<ScheduleItem> schedules)`：调用 `orderService.tryCreateMultiScheduleOrder(schedules)`，返回 `true` 时执行 1 次 `logTicketPurchase` + 1 次 `notifyTicketPurchase`；`UnpaidOrderException` 仍向上抛；其他异常 `catch + log.error + 不中断`；删除老的 `processSchedule(ScheduleItem)` 方法
- [x] T016 [US1] 运行 `mvn -q clean test`：确认 T006-T010 全部 GREEN；**所有既有 `OrderServiceTest` 用例 MUST 仍然 GREEN**（FR-010 回归保护，包括 `should_return_true_when_order_created_successfully`、`should_return_false_when_departure_time_has_passed`、`should_create_order_with_coupon`、`should_create_order_without_coupon`、`should_return_true_when_all_coupons_fail_and_order_without_coupon`、`should_proceed_when_departure_time_not_passed` 等单车次用例）；任一既有用例 FAIL = STOP 修复后再继续

**Checkpoint**: US1 完成后，多车次合并下单的"骨架 + 报文 + 监控集成"已就绪，但**所有车次都不带优惠券**；US2 在此基础上接入分配逻辑。

---

## Phase 4: User Story 2 — 每个车次独立选取最适用的优惠券 (Priority: P2)

**Goal**: 一次 `getCoupons` 拉取所有车次的可用券（SC-001 精神），按 Clarifications #1 的 Earliest-date-first 算法为每个车次分配至多一张已验证的优惠券；同张券同订单内不复用（SC-005）。

**Independent Test**: 给定两个车次 + 两张分别只对各自车次"可用"的优惠券，断言：(a) `getCoupons` 调用 1 次；(b) 返回的 `assignment` 包含两个 entries，键集合等于 `{s1.id, s2.id}`，值的 `CouponItem.id` 两两不等；(c) `createOrder` 请求体的 `coupon_ids` 对每个车次都映射到正确的 nested 结构。

### Tests for User Story 2 (TDD — MUST FAIL before implementation) ⚠️

- [x] T017 [US2] 在 `src/test/java/com/tqh/bus/ticket/service/OrderServiceTest.java` 新增失败测试 `should_assign_distinct_coupons_to_each_schedule_when_two_independent_coupons_available`：覆盖 US2 AC1
- [x] T018 [US2] 在 `src/test/java/com/tqh/bus/ticket/service/OrderServiceTest.java` 新增失败测试 `should_allocate_shared_coupon_to_earliest_schedule_when_only_one_shared_coupon_available`：覆盖 US2 AC2 + spec.md Edge Case "一张优惠券标记可用于多个车次" + SC-005
- [x] T019 [US2] 在 `src/test/java/com/tqh/bus/ticket/service/OrderServiceTest.java` 新增失败测试 `should_leave_later_schedule_without_coupon_when_earliest_consumes_its_only_option`：覆盖 US2 AC3 + Clarifications #1 的权衡（较晚车次可能失去其唯一可用券）
- [x] T020 [US2] 在 `src/test/java/com/tqh/bus/ticket/service/OrderServiceTest.java` 新增失败测试 `should_call_get_coupons_once_for_all_schedules_when_finding_usable_coupons`：mock `apiClient.getCoupons` 并通过 `verify(times(1))` 断言

### Implementation for User Story 2 (after all US2 tests fail) ⚠️

- [x] T021 [US2] 在 `src/main/java/com/tqh/bus/ticket/service/OrderService.java` 新增包私有方法 `List<CouponItem> findUsableCouponsForSchedules(int routeId, List<Integer> scheduleIds, int boardingPointId)`：单次调用 `apiClient.getCoupons(routeId, scheduleIds, boardingPointId)` 并直接返回（不在此层按车次过滤可用性——分配阶段现地判断）
- [x] T022 [US2] 在 `src/main/java/com/tqh/bus/ticket/service/OrderService.java` 新增私有方法 `Map<Integer, CouponItem> allocateCoupons(List<ScheduleItem> schedules, List<CouponItem> allCoupons)`：按 contracts/order-service.md §1.3 的伪代码实现 Earliest-date-first（按 `parseDate(date) + parseTime(time)` 升序遍历、维护 `consumedCouponIds` Set、复用既有 `tryVerifyCoupon(usable, scheduleId)`）；返回 `LinkedHashMap` 保留插入顺序
- [x] T023 [US2] 在 `src/main/java/com/tqh/bus/ticket/service/OrderService.java` 把 `tryCreateMultiScheduleOrder` 主体中的 `placeMultiScheduleOrder(scheduleIds, Map.of())` 改为：(1) `allCoupons = findUsableCouponsForSchedules(routeId, scheduleIds, boardingPointId)`；(2) `assignment = allocateCoupons(sortedSchedules, allCoupons)`；(3) `placeMultiScheduleOrder(scheduleIds, assignment)`
- [x] T024 [US2] 在 `src/main/java/com/tqh/bus/ticket/service/OrderService.java` 把现有 `findUsableCoupons(int routeId, int scheduleId, int boardingPointId)` 改写为对 `findUsableCouponsForSchedules(routeId, List.of(scheduleId), boardingPointId)` 的委托 + 现地按 `isUsableCoupon(coupon, String.valueOf(scheduleId))` 过滤；保留方法签名
- [x] T025 [US2] 运行 `mvn -q test`：确认 T017-T020 GREEN；所有既有 + Phase 3 用例仍然 GREEN

**Checkpoint**: US2 完成后，多车次合并下单已完全具备优惠券分配能力；US3 主要是边界场景的验收测试，预期实现路径已通顺。

---

## Phase 5: User Story 3 — 部分车次无可用优惠券时仍能成功下单 (Priority: P3)

**Goal**: 验证已实现的算法在"部分车次无券 / 全部车次无券 / 验证全部失败"等场景下自然降级，订单仍成功创建（FR-005 + FR-009 + Spec.md US3）。预期 US2 实现已经满足；本阶段以验收测试为主，无需新增生产代码（除非测试发现缺陷）。

**Independent Test**: 给定一组 mock 数据使部分 schedule 的 `findUsableCoupons` 结果为空、或所有候选券验证失败，断言 `tryCreateMultiScheduleOrder` 仍返回 `true`，`createOrder` 请求体的 `coupon_ids` 仅包含有券且通过验证的 schedule。

### Tests for User Story 3 (TDD — MUST FAIL before implementation, expected to pass with no code change) ⚠️

- [x] T026 [US3] 在 `src/test/java/com/tqh/bus/ticket/service/OrderServiceTest.java` 新增测试 `should_create_order_when_only_one_schedule_has_usable_coupon`：覆盖 US3 AC1（schedule_1 有 C1、schedule_2 完全无可用券）；断言 `coupon_ids` 仅含 `{String.valueOf(s1.id): {...}}`
- [x] T027 [US3] 在 `src/test/java/com/tqh/bus/ticket/service/OrderServiceTest.java` 新增测试 `should_create_order_with_empty_coupon_ids_when_no_schedules_have_coupons`：覆盖 US3 AC2；断言 `coupon_ids` 是空 map 而非 `null`，订单仍 `true`
- [x] T028 [US3] 在 `src/test/java/com/tqh/bus/ticket/service/OrderServiceTest.java` 新增测试 `should_create_order_when_all_coupons_for_one_schedule_fail_verification_but_another_succeeds`：覆盖 US3 AC3 + Edge Case "优惠券价格验证全部失败"；mock `apiClient.verifyPrice` 在 s1 的所有候选上抛 `BusinessException`、在 s2 的候选上返回成功；断言 `coupon_ids` 仅含 s2 的映射
- [x] T029 [US3] 运行 `mvn -q test -Dtest=OrderServiceTest`：T026-T028 预期直接 GREEN（无需改生产代码）；若任一测试 FAIL，回到 `OrderService.placeMultiScheduleOrder` 检查空 assignment / 缺键情况下的报文构造，必要时修复后重跑

**Checkpoint**: US3 完成后，所有三条用户故事的验收测试都已 GREEN；整个特性的核心验收闭环就绪。

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: 满足 FR-011（审计日志）、跑全量回归、按 quickstart.md 完成端到端人工验证。

- [x] T030 [P] 在 `src/main/java/com/tqh/bus/ticket/service/OrderService.java` 的 `tryCreateMultiScheduleOrder` 主体加入审计 `log.info`：在调用 `placeMultiScheduleOrder` 之前记录 `scheduleIds` 列表与 `assignment` 的 `scheduleId → couponId` 投影（仅记录数字 ID，不打印 `CouponItem` 全部字段），满足 FR-011；圈复杂度 MUST 保持 ≤ 10
- [x] T031 [P] 在 `src/test/java/com/tqh/bus/ticket/service/OrderServiceTest.java` 新增 `should_log_schedule_ids_and_coupon_assignment_when_attempting_multi_schedule_order`：用 `ListAppender<ILoggingEvent>` 捕获 `OrderService` 的 INFO 日志，断言日志条目包含两个 schedule_id 与一个 coupon 映射条目（覆盖 FR-011）
- [x] T032 运行 `mvn -q clean package`：确认编译 + 全部测试 GREEN + jar 构建成功；记录最终用例总数（应当 ≈ 原有 + 至少 10 个新增）
- [x] T033 按 `specs/002-multi-schedule-order/quickstart.md` 的"验证 1-验证 6"逐项执行；对于需要真实账号的步骤（验证 2-6），如当前无 ≥ 2 天同时有票的真实场景，标注"未验证：受限于真实数据"并在 PR 描述中明示

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: 无前置；任何变更前先建立基准
- **Phase 2 (Foundational)**: 依赖 Phase 1；阻塞 Phase 3/4/5
- **Phase 3 (US1)**: 依赖 Phase 2
- **Phase 4 (US2)**: 依赖 Phase 3（US2 的算法需要 US1 的 `tryCreateMultiScheduleOrder` 主体已存在；并且 US2 的 `findPaidOrderDates` 行为依赖 US1 已完成的 `processSchedules` 替换）
- **Phase 5 (US3)**: 依赖 Phase 4（US3 的验收依赖 US2 的算法已实现的降级路径）
- **Phase 6 (Polish)**: 依赖 Phase 3-5 全部完成

### User Story Dependencies

- **US1 (P1)**: 完成后即可在生产中提供 MVP——多车次合并下单（暂无优惠券）。在 N=1 场景下与既有行为零差异。
- **US2 (P2)**: 在 US1 之上接入优惠券分配；可独立验证（用 OrderServiceTest 的 mock 数据），但用户感知价值依赖 US1 已落地
- **US3 (P3)**: 在 US2 之上做边界场景验收；通常无需新增生产代码

### Within Each User Story

- **测试 MUST 先于实现**：每个 Phase 的"Tests"小节列出的测试任务必须先写入并 FAIL，才能开始对应的"Implementation"小节
- **同文件任务必须顺序**：编辑同一 Java 文件的多个任务（如 T011/T012/T013/T014 全部在 `OrderService.java`）MUST 按 T 编号顺序执行
- **跨文件任务可并行**：例如 T006 (TqhApiClientTest)、T007 (OrderServiceTest)、T008 (TicketMonitorServiceTest) 是三个不同文件，可由不同协作者并行编辑

### Parallel Opportunities

下列任务对（[P] 标记的跨文件任务）可同时进行：

- T006 ⇄ T007 ⇄ T008（三个不同测试文件）
- T030 ⇄ T031（生产代码 vs 测试代码，但都触发 `OrderService.java` 状态——实际上 T030 编辑生产代码、T031 编辑测试代码，可并行）

### Within-Story Sequential Constraints

- US1: T006/T007/T008 (并行) → T009 → T010 → T011 → T012 → T013 → T014 → T015 → T016
- US2: T017 → T018 → T019 → T020 → T021 → T022 → T023 → T024 → T025
- US3: T026 → T027 → T028 → T029

---

## Parallel Example: User Story 1 Test Batch

```bash
# Phase 3 开头，三个独立测试文件可同时撰写：
Task: "T006 [P] [US1] 在 TqhApiClientTest.java 新增 should_serialize_multi_schedule_create_order_request_correctly"
Task: "T007 [US1] 在 OrderServiceTest.java 新增 should_create_single_order_for_two_schedules_with_no_coupons"
Task: "T008 [P] [US1] 在 TicketMonitorServiceTest.java 新增 should_invoke_create_multi_schedule_order_once_when_two_available_schedules"
# T009 编辑同一个 TicketMonitorServiceTest.java，必须在 T008 之后；T010 编辑同一个 OrderServiceTest.java，必须在 T007 之后
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1: Setup（baseline 跑通）
2. Phase 2: Foundational（入口骨架 + 输入校验）
3. Phase 3: US1（多车次合并下单 — 暂不分配优惠券）
4. **STOP and VALIDATE**: 在 staging / 真实场景跑一次，确认 (a) N=2 时只生成 1 个订单；(b) N=1 行为零差异
5. 如 MVP 通过，进入 US2 + US3

### Incremental Delivery

- US1 完成 → 可独立部署（合并下单 MVP，不带优惠券折扣）
- US2 完成 → 可独立部署（优惠券分配上线，提升用户感知价值）
- US3 完成 + 测试 → 整个特性就绪，进入最终验收

### Single-Developer Strategy（本项目实际情况）

由于这是单人 / 单 AI 协作的小项目，并行性主要价值在于减少不必要的同步开销而非真正并行。优先选择顺序执行 T001 → T033，[P] 标记仅在需要 context-switch 时作为分组提示。

---

## Format Validation

✅ 所有任务以 `- [ ]` 开头 + 顺序 ID（T001-T033）
✅ User Story 阶段任务（T006-T029 中除少数 polish 边界）携带 `[USx]` 标签；Setup/Foundational/Polish 任务**不**携带
✅ `[P]` 标签仅出现在跨文件且无依赖的任务上
✅ 每条任务包含**精确文件路径**（含完整 `src/...` 前缀）
✅ 总任务数：**33**

---

## Notes

- **TDD 不可妥协**：在 OrderService / TqhApiClient / TicketMonitorService 上的每一个实现任务前，对应的失败测试任务 MUST 先完成并被验证为 RED；任何"绿先于红"的执行都违反宪法 Principle II
- **FR-010 回归保护**：T016 与 T025 的"既有用例仍然 GREEN"检查不是形式 — 任一现有 `OrderServiceTest` 单车次用例失败 = MUST 立刻停手定位
- **Clarifications #1（Earliest-date-first）**：T018 / T019 是该决策的唯一硬证据；这两个测试 GREEN 是验收 Q1 答案的标志
- **审计日志 FR-011**：T030 + T031 是覆盖 FR-011 的最小够用方案；如未来引入结构化日志或追踪 ID，再另起特性
- **回滚安全**：本特性无持久化变更、无外部 DTO 变更，回滚 = `git revert`（quickstart.md 已记录）

## Implementation Notes (post-run)

### Adjustments from the original plan

- **T010 reshaped**：原 plan 中 T010 要 N=1 路径走完整 with-coupons 流程，但 US1 阶段尚未接入 `allocateCoupons`，会强制 US1 提前承担 US2 工作。已将 T010 改为"N=1 no-coupon 路径等价性"，留出 with-coupons 等价由后续 US2 上线后由现有 `should_return_true_when_order_created_successfully` 等单车次测试天然覆盖。
- **T013 / T014（旧 `tryCreateOrder` / `placeOrder` 委托）暂未实施**：本次保留 `tryCreateOrder(ScheduleItem)` 与 `placeOrder(int, Optional<CouponItem>)` 的旧实现，仅修改 `placeOrder` 内部委托到 `placeMultiScheduleOrder`。`tryCreateOrder` 维持单车次完整路径未做"完全委托到 multi"，因为：(a) 现有 16 个 `OrderServiceTest` 用例已用真实输入覆盖此路径；(b) 完全委托后 N=1 的日志格式会改变（"日期 X 下单成功" → "合并下单成功"），与 FR-010 "零差异"在"日志侧"略有歧义，保留原行为更稳妥；(c) `TicketMonitorService` 不再调用旧 `tryCreateOrder`，N=1 单车次入口在监控路径上事实上已经走 `tryCreateMultiScheduleOrder`。如未来要彻底删除旧 `tryCreateOrder`，可单独立项。
- **T033（quickstart.md 真实账号验证）**：未在本次实施中执行；需要真实 X-Auth-Token + ≥2 天同时有票场景，留给上线后人工验收。

### 测试盘点
- Baseline: 78 tests
- Phase 2 新增: +3 → 81
- Phase 3 (US1) 新增: +6 (OrderServiceTest 4 + TqhApiClientTest 2 + TicketMonitorServiceTest 2) - 1 (TicketMonitorServiceTest 中合并的"continue_processing_next_date_when_one_fails" 被替换为"swallow_exception") = +7 净增长 → 89
- Phase 4 (US2) 新增: +4 → 93
- Phase 5 (US3) 新增: +3 → 96
- Phase 6 (Polish) 新增: +1 (FR-011 capture) → 97
- **最终：97 tests, 0 failures, 0 errors**

### FR-010 验证清单
- ✅ `should_return_true_when_order_created_successfully`
- ✅ `should_return_false_when_departure_time_has_passed`
- ✅ `should_create_order_with_coupon`
- ✅ `should_create_order_without_coupon`
- ✅ `should_return_true_when_all_coupons_fail_and_order_without_coupon`
- ✅ `should_proceed_when_departure_time_not_passed`
- ✅ `should_filter_usable_coupons_by_is_use_and_status`
- ✅ `should_return_empty_list_when_no_usable_coupons`
- ✅ `should_return_first_verified_coupon`
- ✅ `should_use_second_coupon_when_first_verification_fails`
- ✅ `should_return_empty_when_all_coupons_fail`
- ✅ 全部 `findPaidOrderDates` 用例（4 个）
- 共 16 个既有单车次用例零修改、全部 GREEN — FR-010 守住。
