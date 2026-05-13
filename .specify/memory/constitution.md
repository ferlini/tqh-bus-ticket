<!--
Sync Impact Report
==================
Version change: TEMPLATE (unfilled) → 1.0.0
Bump rationale: First proper materialization of the constitution from project
  context (./constitution.md + CLAUDE.md). No semantic regression vs. the
  project-root copy; SDD location now formally tracks principles + governance.

Modified principles (initial formalization, sourced from ./constitution.md):
  - I. 简单性原则 (Simplicity) — SOLID as floor, no speculative abstraction,
        dependency discipline, reflection only in framework-layer code.
  - II. 测试先行原则 (Test-First, NON-NEGOTIABLE) — strict Red-Green-Refactor;
        no production code without a failing test; bug fix MUST start with a
        repro test.
  - III. 明确性原则 (Clarity) — naming/cyclomatic/nesting bars, no silent
        catches, unified ResultWrapper + BusinessException + GlobalExceptionHandler.

Added sections:
  - 技术栈与质量基线 (Technical Stack & Quality Baseline)
  - 开发工作流 (Development Workflow — Conventional Commits / branching /
        Code Review gates)
  - Governance (amendment flow, semver policy, compliance checks,
        precedence over CLAUDE.md)

Removed sections:
  - Template placeholders for principles 4 and 5 — intentionally omitted.
    The project's established constitution defines exactly three principles
    and they fully cover the team's non-negotiables.

Templates requiring updates:
  - ✅ .specify/templates/plan-template.md — "Constitution Check" is an
        intentionally-blank slot filled per-feature; no structural change needed.
  - ✅ .specify/templates/spec-template.md — no constitution-driven structural
        change needed.
  - ⚠ .specify/templates/tasks-template.md — line 11 states "Tests are
        OPTIONAL - only include them if explicitly requested". This contradicts
        Principle II (Test-First, NON-NEGOTIABLE) for this project. Follow-up
        TODO below.
  - ✅ CLAUDE.md — already imports principles by reference and uses the same
        three-principle framing; no edit required.
  - ✅ README.md — no constitutional references requiring update.

Follow-up TODOs:
  - TODO(tasks-template): Decide whether to (a) edit
    .specify/templates/tasks-template.md to make test tasks non-optional for
    this project, or (b) handle in /speckit-tasks by always emitting tests.
    Option (a) is preferred; tracked outside this command.
  - TODO(root-constitution-duplication): ./constitution.md (project root) and
    .specify/memory/constitution.md now both exist. Both are consistent at
    v1.0.0. Decide whether to (a) keep the root file as a human-readable
    mirror, (b) replace it with a one-line pointer to the SDD location, or
    (c) delete it. Until resolved, edits MUST be made to both files in the
    same PR to prevent drift.
-->

# tqh-bus-ticket Constitution

## Core Principles

### I. 简单性原则（Simplicity）

**最好的代码，是不需要写的代码。**

- 代码 MUST 遵守 SOLID 原则；SOLID 是防止腐化的底线，而非追求复杂架构的借口。
- 接口、基类、泛型、设计模式 MUST NOT 仅因"将来可能扩展"而引入；引入抽象前必须能指出**两个或以上**需要统一处理的具体场景。
- 接口与单一实现类（如 `XxxService` + `XxxServiceImpl`）MUST NOT 共存，除非存在多态、测试替换或框架代理的具体需求。
- 第三方依赖 MUST 经过必要性、代价、替代性三项评估；JDK 标准库或现有依赖能解决的场景 MUST NOT 引入新依赖。
- 反射 MUST 仅出现在通用工具类、注解解析器或 AOP 切面中；业务层 MUST NOT 手写反射，优先使用 Spring 的 `ReflectionUtils`。

**Rationale**: 复杂度是项目长期维护成本的最大来源。本项目体量较小、业务边界清晰，过早抽象、冗余依赖、隐式反射在此环境下尤其难以偿还。

### II. 测试先行原则（Test-First, NON-NEGOTIABLE）

**未经失败测试验证的需求，不是需求，是猜测。**

- 所有新功能与 Bug 修复 MUST 严格按 Red → Green → Refactor 顺序进行，不得跳过任一步骤。
- 生产代码 MUST NOT 在没有一个失败测试覆盖该行为的前提下被编写或修改。
- 修复 Bug 时 MUST 先编写一个能稳定复现该 Bug 的失败测试，然后再修复代码，以确保该 Bug 永不复现。
- 测试 MUST 是确定性的（相同输入相同结果）、相互独立的（不共享可变状态、不依赖执行顺序）、命名表达意图的（如 `should_throw_when_input_is_null`，禁止 `test1`）。
- 单元测试 SHOULD 占测试金字塔的主体；集成边界（HTTP 客户端、定时任务、外部 API、Webhook）MUST 有专门的集成测试覆盖。

**Rationale**: 本系统自动调用外部 HTTP 接口创建订单，未经测试的修改可能直接造成资金或票务损失。测试先行是防止回归与保证可审计性的唯一可靠手段，因此明确标记为 NON-NEGOTIABLE。

### III. 明确性原则（Clarity）

**代码的首要读者是人，不是编译器。**

- 命名 MUST 准确表达**意图**（业务含义 + 单位），如 `elapsedTimeInDays` 而非 `int d`；魔法数字 MUST 被有意义的常量名替代。
- 单个方法的圈复杂度 MUST NOT 超过 10；嵌套层级 MUST NOT 超过 3 层；超出时 MUST 提取方法或重构逻辑。
- 异常 MUST NOT 被静默吞掉。`catch` 块 MUST 满足以下其一：(a) 用 `BusinessException` 或 `RuntimeException` 包装上下文信息后向外抛出；(b) 是已知可恢复场景，进入降级流程并以注释说明原因。仅记录日志后不抛出的 `catch` MUST NOT 出现于业务代码中。
- 所有 Controller 接口 MUST 返回统一的 `ResultWrapper` 结构 `{code, message, data}`；业务失败 MUST 通过抛出 `BusinessException` 表达，由 `@RestControllerAdvice` 全局处理器统一转换，MUST NOT 以层层传递错误码的方式替代。
- 项目已建立的命名约定、包结构、代码风格 MUST 被严格遵守；个人偏好 MUST NOT 在缺乏团队共识的情况下被引入。
- 注释 MUST 解释"为什么"（业务决策、权衡、非显而易见的约束），MUST NOT 用于复述"是什么"。

**Rationale**: 代码可读性直接决定后续修改速度与缺陷率。统一的异常处理与响应结构既降低客户端处理成本，也避免业务主线被错误处理路径污染；圈复杂度与嵌套上限是这些目标的可量化抓手。

## 技术栈与质量基线

本项目的实现选择具有约束力；偏离 MUST 通过本宪法的修订流程进行，不得在 PR 中以"顺手升级"或"试一下"为由变更。

- **运行时**: Java 21（LTS）
- **核心框架**: Spring Boot 3.4
- **HTTP 客户端**: `RestClient` + Jackson；MUST NOT 引入 OkHttp、Apache HttpClient 等额外客户端，除非存在 Spring 内置组件无法满足的具体需求并经评审。
- **测试栈**: JUnit 5 + Mockito + AssertJ；集成测试使用 `@SpringBootTest` 配合 MockWebServer 或等价方案。
- **构建**: Maven；`mvn clean package` MUST 在无网络副作用的前提下可重复运行。
- **质量门**: 新增或修改的代码 MUST 编译通过且测试全绿；引入的圈复杂度或嵌套深度违规 MUST 在合并前修复。

**Rationale**: 锁定一致的技术栈让"简单性"原则可执行——不同请求库、不同测试框架的混用是隐性复杂度的常见来源。

## 开发工作流

### Conventional Commits（强制）

所有提交 MUST 遵循 [Conventional Commits 1.0.0](https://www.conventionalcommits.org/)：`<type>[scope]: <description>`。
允许的 `type`：`feat`、`fix`、`refactor`、`test`、`docs`、`style`、`perf`、`chore`、`ci`、`revert`。
破坏性变更 MUST 在 `type` 后追加 `!` 并在 footer 用 `BREAKING CHANGE:` 注明影响范围。

### 分支与合入

- `main` 分支 MUST 仅接受经过 Code Review 的 PR；MUST NOT 直接推送。
- 功能分支命名 `feat/<scope>-<description>` 或 `feature-<description>`；修复分支命名 `fix/<scope>-<description>`。
- PR 描述 MUST 说明动机、测试方式、风险点。

### Code Review 质量门

每次 Code Review MUST 覆盖以下维度，并以清单形式输出：

1. **宪法合规**：三条原则是否被遵守？
2. **测试覆盖**：新增或修改代码是否有"失败先行"的测试覆盖？
3. **简单性**：是否引入了不必要的抽象、依赖或反射？
4. **明确性**：命名、圈复杂度、异常处理是否达标？

违反"必须修复"维度的 PR MUST NOT 合入；违反"建议改进"的 SHOULD 记录为后续 issue。

## Governance

1. **优先级**：本宪法 supersedes `CLAUDE.md` 与项目内一切其他指导文档；冲突时 MUST 以本宪法为准。
2. **修订流程**：
   - 任何对原则或本治理章节的修改 MUST 通过 PR 进行，PR 标题以 `docs: amend constitution` 开头。
   - PR 描述 MUST 包含：动机、影响范围、Sync Impact Report、版本号变更理由。
   - 版本号 MUST 遵循语义化版本：
     - **MAJOR**：原则或治理规则的不兼容变更（删除或重定义已有原则、收紧 NON-NEGOTIABLE 范围等）。
     - **MINOR**：新增原则或章节，或对现有原则的实质性扩展。
     - **PATCH**：措辞、错别字、示例细化等非语义变更。
3. **合规检查**：所有 PR 与 Code Review MUST 显式核对宪法合规性；违规处必须在合并前修复，或在所属 `plan.md` 的 `Complexity Tracking` 章节中给出充分理由。
4. **运行时指引**：`CLAUDE.md` 作为运行时协作指南补充使用，其内容 MUST NOT 与本宪法相矛盾；发现矛盾时优先修订 `CLAUDE.md`，而非削弱本宪法。
5. **AI 协作者约束**：AI 协作者在生成任何代码或建议任何架构之前，MUST 以本宪法中的三条原则作为思考起点与检验标准；违反原则的请求 MUST 被指出并给出符合原则的替代方案，而非沉默执行。

**Version**: 1.0.0 | **Ratified**: 2026-03-27 | **Last Amended**: 2026-05-13
