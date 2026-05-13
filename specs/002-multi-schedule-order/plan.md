# Implementation Plan: 多车次合并下单与优惠券支持

**Branch**: `002-multi-schedule-order` | **Date**: 2026-05-13 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-multi-schedule-order/spec.md`

## Summary

把 `OrderService` 的下单流程从"一次一个 `ScheduleItem`"扩展到"一次 N 个 `ScheduleItem`，合并为单个外部订单"。`TqhApiClient` 与现有 HTTP DTO（`CreateOrderRequest.scheduleIds: List<Integer>`、`CreateOrderRequest.couponIds: Map<String, Map<String, Integer>>`、`CouponRequest.scheduleIds: List<Integer>`、`PriceVerificationRequest.scheduleIds: List<Integer>`）已经支持多车次结构，不需要 DTO 改动。本特性的技术核心是：(1) 在 `OrderService` 内新增多车次入口；(2) 实现"按出发日期升序贪心"的优惠券分配算法（Clarifications #1 已敲定）；(3) 在 `TicketMonitorService.executeMonitorCycle` 中把"逐个 schedule 调用 `tryCreateOrder`"改为"过滤后一次性传给新入口"；(4) 严格遵守 FR-010：单车次（N=1）路径必须与现有行为零差异，所有现有 `OrderServiceTest` 用例保持绿灯。

## Technical Context

**Language/Version**: Java 21 (LTS) — 项目宪法约束
**Primary Dependencies**: Spring Boot 3.4.4（spring-boot-starter-web）、Jackson、`RestClient`；测试侧 JUnit 5 + Mockito + AssertJ + Spring `MockRestServiceServer`；**不引入新依赖**
**Storage**: N/A — 状态来自外部 TQH API，购票日志由现有 `TicketLogService` 追加写入文件 `./logs/ticket.log`
**Testing**: 单元测试用 `@ExtendWith(MockitoExtension.class)` mock `TqhApiClient` 与 `TqhProperties`；集成测试用 `MockRestServiceServer` 验证 HTTP 报文（已建立的模式，见 `TqhApiClientTest`）
**Target Platform**: 单实例 JVM 服务（macOS / Linux），通过 `MonitorController` 暴露 REST 端点，监控由 `ScheduledExecutorService` 内驱
**Project Type**: Single-module Maven web service（沿用现有 `tqh-bus-ticket` 模块）
**Performance Goals**: 本特性属于行为变更而非性能优化；目标是**减少**外部 API 调用次数（合并后单轮 `createOrder` 调用从 N 次降到 1 次）。每轮监控周期目标 ≤ 30 秒（既有约束，不变）
**Constraints**: 圈复杂度 ≤ 10（宪法）；嵌套 ≤ 3 层（宪法）；保留所有现有测试绿灯（FR-010 回归保护）；HTTP 客户端复用 `RestClient`，禁止引入 OkHttp/Apache HttpClient（宪法）
**Scale/Scope**: N 的实际上界由 `DateRangeCalculator.thisWeekDates/nextWeekDates` 决定——一周 ≤ 7 个目标日期、每天通常 1 个车次，N 实际范围 1–7；空间复杂度可忽略

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### 原则 I — 简单性原则（Simplicity）

| 检查项 | 状态 | 备注 |
|--------|------|------|
| SOLID 底线 | PASS | `OrderService` 仍是单一职责（下单流程编排）；新方法只是按列表迭代既有单车次能力 |
| 无不必要抽象 | PASS | 不引入 Strategy/Factory 等模式；优惠券分配是一个 private 方法，不是独立类（仅一个使用点） |
| 不为单一实现引入接口 | PASS | 不创建 `CouponAllocator` 接口 + 单一实现这种过度设计 |
| 无非必需依赖 | PASS | 0 新增依赖 |
| 反射边界 | PASS | 不引入反射 |

### 原则 II — 测试先行原则（Test-First, NON-NEGOTIABLE）

| 检查项 | 状态 | 备注 |
|--------|------|------|
| TDD Red→Green→Refactor | PASS | 实施顺序：先为新公开方法（`tryCreateMultiScheduleOrder`、`findUsableCouponsForSchedules`、`allocateCoupons`、`placeMultiScheduleOrder`）编写失败测试，再实现 |
| 单元/集成测试边界 | PASS | `OrderService` 走 Mockito 单元测试；`TqhApiClient` 的请求体结构变化（虽 DTO 不变，但实际承载多 schedule_id 的场景）通过 `MockRestServiceServer` 添加新集成测试 |
| 命名表达意图 | PASS | 例如 `should_allocate_shared_coupon_to_earliest_schedule_when_two_schedules_share_one`、`should_build_multi_schedule_order_request_with_one_coupon_per_schedule` |
| Bug 修复需重现测试先行 | N/A | 这是新特性，非 bug 修复 |

### 原则 III — 明确性原则（Clarity）

| 检查项 | 状态 | 备注 |
|--------|------|------|
| 命名即文档 | PASS | 新方法名直接表达"多车次"语义，参数类型用 `List<ScheduleItem>` 不用模糊的 `Object`/`Collection` |
| 圈复杂度 ≤ 10 | PASS | 算法拆分为 `sortByDate` → `iterateAndAllocate` → `buildRequest` 三个小步骤 |
| 嵌套 ≤ 3 层 | PASS | 双层循环（外层 schedule、内层候选 coupon）后立即提取为 helper 方法 |
| 异常不静默吞掉 | PASS | 既有 `tryVerifyCoupon` 的"逐张试错"语义保留——单张验证失败 `catch + debug + continue`，符合 Clarity §3 第 (b) 类合法降级 |
| `ResultWrapper` / `BusinessException` | N/A | 本特性不引入新 Controller 端点；既有 `MonitorController` 的响应结构不变 |
| 一致性高于个人偏好 | PASS | 沿用项目既有命名（`tryCreateOrder` → `tryCreateMultiScheduleOrder`，与 `findUsableCoupons` → `findUsableCouponsForSchedules` 同形） |

**Gate Decision**: ✅ 全部通过，无需进入 Complexity Tracking。

## Project Structure

### Documentation (this feature)

```text
specs/002-multi-schedule-order/
├── spec.md                  # /speckit-specify + /speckit-clarify 产出
├── plan.md                  # ← 本文件
├── research.md              # Phase 0 输出（决策记录）
├── data-model.md            # Phase 1 输出（实体与状态）
├── quickstart.md            # Phase 1 输出（端到端验证步骤）
├── contracts/
│   └── order-service.md     # Phase 1 输出（OrderService 多车次方法契约）
├── checklists/
│   └── requirements.md      # /speckit-specify 已生成
└── tasks.md                 # /speckit-tasks 之后产出（本命令不生成）
```

### Source Code (repository root)

本特性不改变项目分层；所有变更落在以下文件（既有目录、Single project 结构沿用）：

```text
src/main/java/com/tqh/bus/ticket/
├── service/
│   ├── OrderService.java               # 主要修改：新增多车次方法、保留单车次入口（委托到多车次）
│   └── TicketMonitorService.java        # 修改：processSchedule(item) → processSchedules(List)
├── integration/
│   └── TqhApiClient.java                # 不修改（DTO 已支持多 schedule_id）
└── integration/model/                   # 不修改（CreateOrderRequest/CouponRequest/PriceVerificationRequest 已是多车次结构）

src/test/java/com/tqh/bus/ticket/
├── service/
│   ├── OrderServiceTest.java            # 修改：保留所有现有单车次测试，新增 ~10 个多车次测试
│   └── TicketMonitorServiceTest.java     # 修改：新增"一轮多日期合并下单"测试
└── integration/
    └── TqhApiClientTest.java            # 新增：1 个集成测试覆盖"多 schedule_id 的 createOrder 报文"
```

**Structure Decision**: 沿用现有 single-module Maven 项目结构（`src/main/java`、`src/test/java`）。本特性不引入新包、新分层；纯粹是 `service` 层方法签名与算法的扩展。这与宪法 §技术栈与质量基线"锁定一致的技术栈"原则一致——避免在小型项目中无谓地新建包目录。

## Complexity Tracking

> **未触发** — 合宪性审查全部通过，无需偏离原则的复杂度妥协。表格留空意味着没有借口需要解释。

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|--------------------------------------|
| —         | —          | —                                    |
