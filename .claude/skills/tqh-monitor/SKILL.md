---
name: 通勤号车票监控
description: This skill should be used when the user asks to "监控本周车票", "监控下周车票", "监控车票", "抢票", "买票", "开始监控", "停止监控", "查看购票日志", "有没有新票", mentions specific dates like "监控3月28日车票", "每周五自动抢票", or wants to manage the tqh-bus-ticket monitoring service.
---

# 通勤号车票监控

通勤号巴士车票监控与自动下单系统。监控指定路线车次余票，有票时自动创建订单（不自动支付）。

## 前置条件

- JAR 包路径：`~/tqh-bus-ticket/tqh-bus-ticket-0.0.1-SNAPSHOT.jar`
- 服务端口：`8080`
- 启动脚本：`~/tqh-bus-ticket/start.sh`
- 停止脚本：`~/tqh-bus-ticket/stop.sh`

## 核心工作流

### 启动监控服务

在发起任何监控请求前，确认服务已启动。执行辅助脚本检查并自动启动：

```bash
bash ~/.claude/skills/tqh-monitor/scripts/check-service.sh
```

脚本输出 `RUNNING` 表示已在运行，`STARTED` 表示刚启动成功。

### 监控本周车票

触发词："监控本周"、"本周车票"、"抢本周的票"

```bash
curl -s -X POST http://localhost:8080/monitor/this-week | python3 -m json.tool
```

### 监控下周车票

触发词："监控下周"、"下周车票"、"抢下周的票"

```bash
curl -s -X POST http://localhost:8080/monitor/next-week | python3 -m json.tool
```

### 监控指定日期车票

触发词："监控3月28日"、"监控2026-03-28"、"买3月28号的票"

将用户提到的日期解析为 `yyyy-MM-dd` 格式：

```bash
curl -s -X POST http://localhost:8080/monitor/dates \
  -H "Content-Type: application/json" \
  -d '{"dates": ["2026-03-28"]}' | python3 -m json.tool
```

支持多个日期：

```bash
curl -s -X POST http://localhost:8080/monitor/dates \
  -H "Content-Type: application/json" \
  -d '{"dates": ["2026-03-28", "2026-03-29"]}' | python3 -m json.tool
```

### 停止监控

触发词："停止监控"、"关闭监控"、"停止抢票"

停止监控服务（所有监控都会停止）：

```bash
~/tqh-bus-ticket/stop.sh
```

### 每周五自动监控下周车票

触发词："自动监控下周"、"每周五自动抢票"、"定时监控"

使用 Claude Code 的 schedule 功能，在每周五早上 10 点后触发监控下周车票：

1. 确认服务已启动（执行 check-service.sh）
2. 发起监控请求：
   ```bash
   curl -s -X POST http://localhost:8080/monitor/next-week
   ```

### 监听购票日志通知

触发词："查看购票日志"、"有没有新票"、"监听日志"

每 10 秒检查 `~/tqh-bus-ticket/logs/ticket.log` 是否有新增内容，有则通知用户。执行辅助脚本：

```bash
bash ~/.openclaw/skills/tqh-monitor/scripts/watch-ticket-log.sh
```

脚本会持续运行，检测到新内容时输出新增的行。通过 Claude Code 的 loop 功能实现持续监听：

```
/loop 10s bash ~/.openclaw/skills/tqh-monitor/scripts/watch-ticket-log.sh --once
```

`--once` 参数表示只检查一次是否有新内容并输出，适配 loop 的周期调用。

## 监控发现服务未启动时的处理

当用户请求监控但服务未启动时：

1. 执行 `scripts/check-service.sh` 自动启动服务
2. 等待服务就绪后再发起监控请求
3. 如果启动失败，提示用户检查 JAR 包是否存在

## 响应格式

成功启动监控后，服务返回：

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

向用户展示监控日期列表和轮询间隔。

## 辅助脚本

- `scripts/check-service.sh` — 检查服务状态，未运行时自动启动
- `scripts/watch-ticket-log.sh` — 监听购票日志变化，`--once` 模式检查一次后退出
