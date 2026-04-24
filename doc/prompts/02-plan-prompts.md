# 根据 spec 制定技术架构

## 头脑风暴--技术选型
非常好。基于这份 `spec.md` 和我们的 `constitution.md`（特别是关于包内聚的原则），请为这个功能进行架构设计和技术选型。
1. 使用 SpringBoot 3.x 作为基础架构。
2. 外部 API 请求使用 RestClient 代码库。
3. 外部 API 请求封装为代码库，方便替换。
4. 业务逻辑代码放在`com.tqh.bus.ticket.service`。
5. 外部 API 代码放在`com.tqh.bus.ticket.integration`。

## 制定 plan
**Prompt 1: 生成技术方案**

`@./specs/001-monitor-and-buy-ticket/spec.md`

你现在是`tqh-bus-ticket`项目的首席架构师。你的任务是基于我提供的`spec.md`以及我们已有的`constitution.md`（你已自动加载），为项目生成一份详细的技术实现方案（`plan.md`）。

**技术栈约束 (必须遵循):**

- **语言**: Java (>=21)
- **Web框架**: 使用 SpringBoot 3.x 作为基础架构。
- **HTTP Client**: 外部 API 请求使用 RestClient 代码库。
- **接口数据序列化**: 所有接口的请求/响应报文使用 Spring 自带的 Jackson。

**方案内容要求 (必须包含):**

1.  **技术上下文总结:** 明确上述技术选型。
2.  **“合宪性”审查:** 逐条对照`constitution.md`的原则，检查并确认本技术方案符合所有条款（特别是包内聚、错误处理、TDD）。
3.  **项目结构细化:** 业务逻辑代码放在`com.tqh.bus.ticket.service`，外部 API 代码放在`com.tqh.bus.ticket.integration`。

请严格按照`@./.claude/templates/plan-template.md`的模板格式来组织你的输出（如果模板不存在，请自行设计一个结构清晰的Markdown格式）。

完成后，将生成的`plan.md`内容写入到`./specs/001-monitor-and-buy-ticket/plan.md`文件中。
