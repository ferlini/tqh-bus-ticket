# Quickstart — 验证多车次合并下单与优惠券支持

**Feature**: 002-multi-schedule-order
**Date**: 2026-05-13
**Purpose**: 在 `/speckit-implement` 完成后，按本步骤端到端验证特性符合 spec.md 的 5 个 Success Criteria。

---

## 前置条件

1. 已合并 002-multi-schedule-order 全部代码改动
2. `mvn clean package -DskipTests` 通过
3. `application.yml` 中 `tqh.auth-token` 是有效的 X-Auth-Token
4. 监控目标日期范围内至少有 2 个不同日期都有余票（如果当前外部无 2 天同时有票，等待真实场景或调小路线/扩大日期范围）

---

## 验证 1 — 全部单元测试 + 集成测试通过（守护 FR-010）

```bash
mvn test
```

**通过标准**：
- `OrderServiceTest`：所有既有用例 + 本次新增 ~10 个多车次用例全部 PASS
- `TicketMonitorServiceTest`：既有 + 新增"合并下单单次通知"用例 PASS
- `TqhApiClientTest`：既有 + 新增"多 schedule_id 的 createOrder 报文结构"集成用例 PASS

> 任一现有用例 FAIL = FR-010 回归保护被破坏，必须修复后再继续。

---

## 验证 2 — 监控合并下单（SC-001 / SC-003）

启动服务：

```bash
java -jar target/tqh-bus-ticket-0.0.1-SNAPSHOT.jar &
SERVICE_PID=$!
```

**调用监控**（假设本周二、本周三均有票）：

```bash
curl -X POST http://localhost:8080/monitor/this-week
```

**期望响应**：

```json
{"status": "started", "monitorDates": ["2026-05-13","2026-05-14",...], "interval": 30}
```

**等待一个完整 monitor cycle（≤30s）后**：

1. 打开微信小程序 → "我的订单" → "待支付"
2. **验证 SC-001 + SC-003**：应当只看到 **1 个**新订单，订单中包含 **2 个日期/车次**，而不是 2 个独立订单

如果在外部 API 行为里"合并订单"展现为单条订单且包含多条车次明细，SC-003 通过。

---

## 验证 3 — 单车次场景行为不变（SC-004）

停止监控、清掉订单（或等真实场景里只有 1 个目标日期有票），重新发起：

```bash
curl -X POST http://localhost:8080/monitor/stop
# 等待已支付订单被处理或只剩 1 天有票的场景
curl -X POST http://localhost:8080/monitor/this-week
```

**等待 1 个 cycle**后：

- 微信小程序中创建出 **1 个** 单车次订单，与 001 阶段的体验完全一致
- `logs/ticket.log` 内的日志条目格式与既有相同（既有 `TicketLogService.formatLog` 不变）
- OpenClaw webhook 收到 1 条购票成功通知（既有文案模板）

---

## 验证 4 — 共享优惠券分配到较早车次（SC-005）

**前置**：账户中至少有一张优惠券 `C`，且 `C.isUse` 对周二与周三两个目标车次都为 `true`、`C.status` 都为 `待使用`。

发起合并监控（同验证 2 步骤），等待下单成功。

**检查微信小程序中刚生成的合并订单**：

- 仅 **一个车次**（按发车日期较早的那个）使用了优惠券 `C`
- 较晚的那个车次未使用优惠券
- 账户优惠券列表中：`C` 已变为"已使用"（消费次数 +1 而非 +2）—— 满足 SC-005

---

## 验证 5 — 部分车次无券仍下单成功（US3）

**前置**：账户中仅一张优惠券且只对周二车次可用、周三车次没有任何可用券。

发起合并监控、等待 cycle 完成。

**期望**：

- 1 个合并订单创建成功
- 该订单的"已抵扣金额"显示 = 周二车次应用了优惠券的折扣金额（非 0）
- 该订单的总金额 = 两张票价 - 周二车次折扣
- 周三车次按原价

---

## 验证 6 — 日志与 webhook 单次通知（FR-006）

`tail -f logs/ticket.log` 在验证 2 后应当只看到 **1 条**新增日志块（覆盖 2 个日期），不是 2 个日志块。

OpenClaw webhook 端（或测试用 mock 端点）只收到 **1 个** `notifyTicketPurchase` 请求，文案中列出 2 个日期。

---

## 异常路径快速验证

| 场景 | 操作 | 期望 |
|------|------|------|
| 有待支付订单时启动监控 | 保留小程序里 1 笔待支付订单，发起 monitor | 监控自动 stop；`logs/unpaid-warning.log` 出现告警；不发起任何 createOrder |
| 全部目标日期均已支付 | 提前已支付所有目标日期 | 监控空跑（无 createOrder 调用），下一轮重试 |
| 服务过程中遇到外部 API 5xx | 模拟用 token 失效 | 当前 cycle 日志 ERROR；下一轮自动重试；不进入"半个订单"状态 |

---

## 回滚指引

如果生产中发现多车次合并下单导致重大问题，回滚策略：

1. `git revert <merge-commit-of-002>` 即可（本特性不修改任何 DTO、不涉及持久化迁移）
2. 重新 `mvn clean package -DskipTests` + 重启
3. 行为立即回到 001 阶段的"每车次独立下单"

> 因为外部 DTO 没有任何破坏性变化、且本服务无持久状态，**回滚是安全的、原子的、无需数据修复**。

---

## 上线验收清单

- [ ] `mvn test` 全绿（含全部既有 + 新增用例）
- [ ] 验证 2 — 微信小程序里观察到合并订单数 = 1（实际 N=2 场景）
- [ ] 验证 3 — N=1 场景与既有体验无差异
- [ ] 验证 4 — 共享券分配给较早车次（仅当账户有此类券）
- [ ] 验证 5 — 部分无券混合订单仍创建成功
- [ ] 验证 6 — 单条日志 + 单次 webhook
- [ ] `logs/ticket.log` 格式未退化
- [ ] OpenClaw webhook 通知文案包含全部日期
