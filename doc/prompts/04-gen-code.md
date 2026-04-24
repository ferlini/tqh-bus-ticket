# 根据 task 生成代码

## 按 RED-GREEN-REFACTOR 的节奏生成代码

### RED （写测试代码）
`@./specs/001-monitor-and-buy-ticket/tasks.md`
`@./specs/001-monitor-and-buy-ticket/plan.md`

请按`tasks.md`计划执行 **Phase 1: 基础设施** 中的所有任务。
1. 外部服务使用`@MockBean`来创建 Mockito 对象进行单元测试。

请严格遵循TDD流程，**先不要实现功能代码**。

### GREEN（让功能代码通过测试）
测试已失败，正如预期。

现在执行 ** Phase 1 ** 的任务。

**要求：**
1.  逻辑必须能通过刚才编写的所有测试用例。
2.  严格遵循 `spec.md` 的要求。
3.  不要过度设计，只写能通过测试的代码。

### REFACTOR（如有需要）
代码通过了测试，找出不遵循 `constitution.md` 和 `spec.md`的地方进行重构。

请进行重构：
1.
2.
3.

重构完成后，**必须**再次运行测试，确保没有破坏现有逻辑。

## 审核生成的代码
**生成的所有代码必须审核。**
