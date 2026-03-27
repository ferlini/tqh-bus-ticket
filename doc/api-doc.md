## API 列表

| 接口名称 | 请求方法 | 路径 | 说明 | 文档路径 |
|---------|---------|------|------|----------|
| 查询车次库存 | POST | `/api/v1/route/schedule/find` | 查询指定路线的车次及车票数量 | `doc/requests/find-schedule.md` |
| 查询可用优惠券 | POST | `/api/v2/order/coupon` | 查询指定车次可用的优惠券列表 | `doc/requests/coupons.md` |
| 验证订单价格 | POST | `/api/v2/order/price/verification` | 验证使用优惠券后的订单价格 | `doc/requests/price-verfication.md` |
| 创建订单 | POST | `/api/v2/order/create` | 创建车票订单 | `doc/requests/create-order.md` |
| 订单列表 | POST | `/api/v1/order/get` | 获取订单列表（支持状态筛选） | `doc/requests/get-order.md` |
| 未支付订单列表 | POST | `/api/v1/order/get` | 获取待支付状态的订单 | `doc/requests/unpaid-order.md` |
| 查找车票 | POST | `/api/v1/route/ticket/find` | 查询用户已购的车票 | `doc/requests/find-tickets.md` |
| 用户信息 | POST | `/api/v1/user/get/info` | 获取当前用户信息 | `doc/requests/get-user-info.md` |
| 路线站点 | POST | `/api/v1/route/timetable/stops` | 查询路线的上车站和下车站列表 | `doc/requests/route-stops.md` |
| 发送 tids | POST | `/api/v1/user/send/tids` | 发送 tids | `doc/requests/send-tids.md` |

---

## 请求头说明

所有 API 请求需包含以下公共请求头：

| 请求头 | 说明 | 示例值 |
|--------|------|--------|
| `X-Auth-Token` | 认证 Token | `xxxxxx` |
| `content-type` | 内容类型 | `application/json` |
| `User-Agent` | 用户代理 | `Mozilla/5.0 (iPhone; CPU iPhone OS 18_0_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.69(0x18004539) NetType/WIFI Language/zh_CN` |
| `Referer` | 来源 | `https://servicewechat.com/wx98d802f3b76e9491/24/page-frame.html` |

---

## 基础 URL

```
https://api.com
```