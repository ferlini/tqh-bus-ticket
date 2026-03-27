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

```bash
# 监控本周（今天至周日）
curl -X POST http://localhost:8080/monitor/this-week

# 监控下周（周一至周日）
curl -X POST http://localhost:8080/monitor/next-week

# 监控指定日期
curl -X POST http://localhost:8080/monitor/dates \
  -H "Content-Type: application/json" \
  -d '{"dates": ["2026-03-25", "2026-03-26"]}'
```

响应示例：

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
