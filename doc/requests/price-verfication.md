# 验证订单价格

## Request URL
```
https://api.com/api/v2/order/price/verificationhttps://api.com/api/v2/order/price/verification
```

## Request Header
```
POST /api/v2/order/price/verification HTTP/1.1
Host: api.com
Connection: keep-alive
Content-Length: 124
X-Auth-Token: xxxxxx
content-type: application/json
Accept-Encoding: gzip,compress,br,deflate
User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 18_0_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.69(0x18004539) NetType/WIFI Language/zh_CN
Referer: https://servicewechat.com/wx98d802f3b76e9491/24/page-frame.html
```

## Request Body
```
{"route_id":275,"schedule_ids":[61429],"coupon_ids":{"61429":{"2":8317178}},"boarding_point_id":24,"alighting_point_id":400}
```

## Response Body
```
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "original_price": 13.0,
    "discount": 10.0,
    "final_price": 3.0
  }
}
```
