# Specification Quality Checklist: 多车次合并下单与优惠券支持

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-05-13
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no implementation details)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

## Notes

- 校验通过，所有验证项一次性满足，无需补充澄清。
- 已就以下事项在 spec.md 的 Assumptions 中显式记录默认决定，避免遗留 [NEEDS CLARIFICATION]：
  - 优惠券一次性使用（不跨车次复用）。
  - 价格验证采用 per-(schedule, coupon) 逐张策略，与现有单车次行为一致。
  - 同券冲突时按车次出发日期升序分配（Earliest-date-first，由 Clarifications #1 决定）。
  - 本特性不引入新的对外入口，仅扩展监控 cycle 与 OrderService。
- 这些默认值若未来证伪，应通过修订本 spec 而非临时偏离实现。
- **Clarifications 后续记录**: 2026-05-13 通过 /speckit-clarify 解决了 US2 AC2/AC3 与 Assumptions 之间关于"同券冲突分配策略"的内部矛盾，统一为 Earliest-date-first。

## Content Quality Spot Checks

Quick re-read sanity check that the spec stays on the WHAT/WHY side:

- Functional Requirements 中提及 `coupon_ids` 的 Map 结构（FR-005）是为了与外部 API 契约对齐，属于"提交格式约束"，非实现细节，保留。
- Functional Requirements 中提及 `UnpaidOrderException` 与 `OrderService`（FR-008、Assumptions）是引用本项目既有领域概念，避免歧义；不构成对实现路径的强约束。
- Success Criteria 全部以"调用次数 / 订单数 / 映射条目数 / 实际消费次数"的可量化外部可观测指标表达，未涉及代码内部结构。
