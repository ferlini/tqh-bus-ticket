# tqh-bus-ticket

通琴号巴士车票监控与自动下单系统。通过本地 curl 命令启动监控，系统自动检测有票后创建订单，用户在微信小程序中手动完成支付。

## 技术栈

- Java 21
- Spring Boot 3.4
- RestClient + Jackson
- JUnit 5 + Mockito

## 快速开始

### 1. 配置 Token

编辑 `src/main/resources/application.yml`，将 `auth-token` 替换为你的微信小程序 X-Auth-Token：

```yaml
tqh:
  base-url: https://api.com
  auth-token: "你的Token"
  route-id: 275
  boarding-point-id: 24
  alighting-point-id: 400
  monitor-interval: 30
```

### 2. 构建与运行

```bash
mvn clean package -DskipTests
java -jar target/tqh-bus-ticket-0.0.1-SNAPSHOT.jar
```

### 3. 启动监控

系统提供两种工作模式，对应两组接口：

| 模式 | 行为 | 端点前缀 |
|------|------|---------|
| **买票（monitor）** | 发现有票即自动创建订单（不自动支付），持续运行直到用户停止或出现待支付订单 | `/monitor/...` |
| **监控（watch）** | 发现有票即聚合所有有票日期发送一条 OpenClaw Webhook 通知，**通知成功后自动停止本次监控** | `/monitor/watch/...` |

#### 买票模式（自动下单）

```bash
# 买本周车票（今天至周日）
curl -X POST http://localhost:8080/monitor/this-week

# 买下周车票（周一至周日）
curl -X POST http://localhost:8080/monitor/next-week

# 买指定日期车票
curl -X POST http://localhost:8080/monitor/dates \
  -H "Content-Type: application/json" \
  -d '{"dates": ["2026-03-25", "2026-03-26"]}'
```

#### 监控模式（仅通知，不下单）

发现任意目标日期有票后，会构造形如：

```
线路A
  - 2026-05-02: 剩余10张
  - 2026-05-03: 剩余5张
```

的内容并通过 OpenClaw Webhook 推送。当 Webhook 返回 `{"ok": true}` 视为发送成功，调度器随即停止；否则下一轮继续重试。

```bash
# 监控本周车票
curl -X POST http://localhost:8080/monitor/watch/this-week

# 监控下周车票
curl -X POST http://localhost:8080/monitor/watch/next-week

# 监控指定日期车票
curl -X POST http://localhost:8080/monitor/watch/dates \
  -H "Content-Type: application/json" \
  -d '{"dates": ["2026-05-02", "2026-05-03"]}'
```

> 「买票」和「监控」共用同一个调度器，发起新任务会自动取消上一个任务，无需手动停止。

响应示例（两种模式相同）：

```json
{
  "status": "started",
  "monitorDates": ["2026-03-25", "2026-03-26"],
  "interval": 30
}
```

## 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `tqh.base-url` | `https://api.com` | 第三方 API 基础地址 |
| `tqh.auth-token` | - | 微信小程序 X-Auth-Token（手动配置） |
| `tqh.route-id` | `275` | 监控路线 ID |
| `tqh.boarding-point-id` | `24` | 上车站 ID |
| `tqh.alighting-point-id` | `400` | 下车站 ID |
| `tqh.monitor-interval` | `30` | 轮询间隔（秒） |

## 下单流程

1. **查询车次库存** — 筛选有票（`number > 0`）且在监控日期范围内的车次
2. **检查已购票** — 按路线名称 + 日期匹配，有未支付或已支付订单则跳过
3. **查询可用优惠券** — 筛选可用的优惠券
4. **验证订单价格** — 依次尝试每张优惠券，全部失败则不使用优惠券
5. **创建订单** — 不自动支付，用户在微信小程序中手动完成
6. **记录日志** — 写入 `./logs/ticket.log`

## 购票日志

成功下单后自动写入 `./logs/ticket.log`，每周五 10:00 自动清空：

```
----------------------------------------
日期: 2026-03-24 07:40:00
路线: xxx
上车站: xxx
下车站: xxx
----------------------------------------
```

## 运行测试

```bash
mvn test
```
