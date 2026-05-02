---
name: 通勤号车票监控
description: This skill should be used when the user asks to "买本周车票", "买下周车票", "抢票", "买票" (purchase mode — auto creates order), or "监控本周车票", "监控下周车票", "监控车票", "有票通知", "继续监控X月X日车票" (watch-only mode — notify when tickets are available, no purchase). Also "停止监控", date phrases like "买3月28日车票", "监控3月28日车票", "每周五自动抢票", or any management of the tqh-bus-ticket service.
---

# 通勤号车票监控

通勤号巴士车票监控系统，提供两种工作模式：

| 模式 | 用途 | 触发词关键字 | 端点前缀 |
|------|------|-------------|---------|
| **买票（monitor）** | 监控有票后自动创建订单（不自动支付） | "买"、"抢票"、"抢本周"、"自动抢票" | `/monitor/...` |
| **监控（watch）** | 监控有票后发送 OpenClaw Webhook 通知，**发送成功后自动停止** | "监控"、"有票通知"、"看有没有票" | `/monitor/watch/...` |

> ⚠️ **关键区别**：
> - 「买票」会持续监控直到下单或服务被停止。
> - 「监控」一旦发现任意目标日期有票并成功发送通知，就会自动结束。如果用户希望再次监控，必须说「继续监控X月X日车票」。

## 前置条件

- JAR 包路径：`~/tqh-bus-ticket/tqh-bus-ticket-0.0.1-SNAPSHOT.jar`
- 服务端口：`8080`
- 启动脚本：`~/tqh-bus-ticket/start.sh`
- 停止脚本：`~/tqh-bus-ticket/stop.sh`

## 核心工作流

### 启动监控服务

在发起任何监控/买票请求前，确认服务已启动：

```bash
bash ~/.openclaw/skills/tqh-monitor/scripts/check-service.sh
```

脚本输出 `RUNNING` 表示已在运行，`STARTED` 表示刚启动成功。

---

## 一、买票模式（自动下单）

> 触发词："买票"、"抢票"、"抢本周的票"、"买X月X日车票"、"自动抢票"。
> 行为：发现有票即创建订单（不自动支付），并发送购票成功 Webhook 通知。
> 停止条件：用户主动停止 / 出现待支付订单。

### 买本周车票

触发词："买本周"、"抢本周的票"、"本周买票"

```bash
curl -s -X POST http://localhost:8080/monitor/this-week | python3 -m json.tool
```

### 买下周车票

触发词："买下周"、"抢下周的票"、"下周买票"

```bash
curl -s -X POST http://localhost:8080/monitor/next-week | python3 -m json.tool
```

### 买指定日期车票

触发词："买3月28日"、"抢2026-03-28"、"买3月28号的票"

```bash
curl -s -X POST http://localhost:8080/monitor/dates \
  -H "Content-Type: application/json" \
  -d '{"dates": ["2026-03-28"]}' | python3 -m json.tool
```

多日期：

```bash
curl -s -X POST http://localhost:8080/monitor/dates \
  -H "Content-Type: application/json" \
  -d '{"dates": ["2026-03-28", "2026-03-29"]}' | python3 -m json.tool
```

### 每周五自动买下周车票

触发词："自动抢下周"、"每周五自动抢票"、"定时抢票"

使用 Claude Code 的 schedule 功能，每周五早上 10 点：
1. 执行 check-service.sh 确保服务启动
2. `curl -s -X POST http://localhost:8080/monitor/next-week`

---

## 二、监控模式（仅通知，不下单）

> 触发词："监控"、"有票通知"、"看有没有票"、"监控X月X日车票"。
> 行为：发现任意目标日期有票即聚合所有有票日期，发送一条 OpenClaw Webhook 通知（线路名 + 日期 + 剩余票数），通知成功后**自动停止本次监控**。
> 通知失败（网络故障 / `{"ok": true}` 未返回）时下一轮继续重试。

### 监控本周车票

触发词："监控本周"、"本周有票通知"

```bash
curl -s -X POST http://localhost:8080/monitor/watch/this-week | python3 -m json.tool
```

### 监控下周车票

触发词："监控下周"、"下周有票通知"

```bash
curl -s -X POST http://localhost:8080/monitor/watch/next-week | python3 -m json.tool
```

### 监控指定日期车票

触发词："监控3月28日车票"、"监控2026-03-28"、"看3月28号有没有票"

```bash
curl -s -X POST http://localhost:8080/monitor/watch/dates \
  -H "Content-Type: application/json" \
  -d '{"dates": ["2026-03-28"]}' | python3 -m json.tool
```

多日期：

```bash
curl -s -X POST http://localhost:8080/monitor/watch/dates \
  -H "Content-Type: application/json" \
  -d '{"dates": ["2026-03-28", "2026-03-29"]}' | python3 -m json.tool
```

### 继续监控（再次启动 watch）

监控模式发出通知后会**自动停止**。当用户希望恢复监控时，必须明确说出：「继续监控X月X日车票」、「继续监控本周车票」、「继续监控下周车票」。识别到此意图时，重新调用对应的 `/monitor/watch/...` 端点（与首次调用相同）。

> 提示：通知到达后请提醒用户：「已发送有票通知，监控已自动停止。如需继续监控，请说『继续监控X月X日车票』。」

---

## 停止监控（两种模式共用）

触发词："停止监控"、"关闭监控"、"停止抢票"

停止当前服务进程（所有买票 / 监控任务都会停止）：

```bash
~/tqh-bus-ticket/stop.sh
```

> 注意：服务内部「买票」与「监控」共享同一个调度器，发起新任务会自动取消上一个任务，无需手动停止。

## 监控发现服务未启动时的处理

当用户请求时若服务未启动：

1. 执行 `scripts/check-service.sh` 自动启动服务
2. 等待服务就绪后再发起请求
3. 启动失败时提示用户检查 JAR 包是否存在

## 响应格式

无论买票还是监控模式，成功启动后服务返回相同结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "status": "started",
    "monitorDates": ["2026-03-28", "2026-03-29"],
    "interval": 30
  }
}
```

向用户展示日期列表与轮询间隔。如果是监控模式，额外提示「发现有票后将发送 Webhook 并自动停止」。

## 辅助脚本

- `scripts/check-service.sh` — 检查服务状态，未运行时自动启动
