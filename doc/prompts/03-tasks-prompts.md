# 制定 task

## 根据 plan 生成 task
方案非常完美。

现在，请扮演技术组长。请仔细阅读 `@./specs/001-monitor-and-buy-ticket/spec.md` 和 `@./specs/001-monitor-and-buy-ticket/plan.md`。

你的目标是将 `plan.md`中描述的技术方案，分解成一个**详尽的、原子化的、有依赖关系的、可被AI直接执行的任务列表**。

**关键要求：**
1.  **任务粒度：** 每个任务应该只涉及一个主要文件的修改或创建一个新文件。不要出现“实现所有功能”这种大任务。
2.  **TDD强制：** 根据`constitution.md`的“测试先行铁律”，**必须**先生成测试任务，后生成实现任务。
3.  **并行标记：** 对于没有依赖关系的任务，请标记 `[P]`。
4.  **阶段划分：** 按照`plan.md`的中步骤实现。
    *   **Phase 1: 基础设施**
    *   **Phase 2: 外部 API 层**
    *   **Phase 3: 业务逻辑层**
    *   **Phase 4: 控制器层**

完成后，将生成的任务列表写入到`./specs/001-monitor-and-buy-ticket/tasks.md`文件中。