# 订单列表

## Request URL

## Request Header
```
POST /api/v1/order/get HTTP/1.1
Host: api.com
Connection: keep-alive
Content-Length: 33
X-Auth-Token: xxxxxx
content-type: application/json
Accept-Encoding: gzip,compress,br,deflate
User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 18_0_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.69(0x18004539) NetType/WIFI Language/zh_CN
Referer: https://servicewechat.com/wx98d802f3b76e9491/24/page-frame.html
```
## Request Body
```
{"status":"","page":1,"limit":10}
```

## Response Body
```
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 572468,
      "coupon_ids": [],
      "price_total": 13.0,
      "ticket_ids": [
        932762
      ],
      "out_trade_no": "17742740540000572467",
      "route_name": "17号线-明珠线-上班",
      "description": {
        "start_stop": "长沙圩①",
        "end_stop": "科创中心西门",
        "date": [
          "2026-03-24 07:40:00"
        ]
      },
      "total": 3.0,
      "trade_state": "支付成功",
      "success_time": "2026-03-23 21:54:26",
      "timeout_time": "2026-03-23 21:54:14",
      "payer_total": 3.0
    },
    {
      "id": 571764,
      "coupon_ids": [],
      "price_total": 13.0,
      "ticket_ids": [
        931932
      ],
      "out_trade_no": "17742612990000571763",
      "route_name": "4号线-上冲/香洲-下班",
      "description": {
        "start_stop": "厚朴道中(科创中心) ②",
        "end_stop": "蓝盾路口②",
        "date": [
          "2026-03-23 18:25:00"
        ]
      },
      "total": 3.0,
      "trade_state": "支付成功",
      "success_time": "2026-03-23 18:21:45",
      "timeout_time": "2026-03-23 18:21:39",
      "payer_total": 3.0
    },
    {
      "id": 562872,
      "coupon_ids": [],
      "price_total": 13.0,
      "ticket_ids": [
        913574
      ],
      "out_trade_no": "17740706220000562871",
      "route_name": "5号线-明珠-上班",
      "description": {
        "start_stop": "云顶澜山",
        "end_stop": "环岛北路西（科创中心） ①",
        "date": [
          "2026-03-23 07:06:00"
        ]
      },
      "total": 13.0,
      "trade_state": "已关闭",
      "success_time": null,
      "timeout_time": "2026-03-21 13:23:42",
      "payer_total": 0.0
    },
    {
      "id": 561069,
      "coupon_ids": [],
      "price_total": 13.0,
      "ticket_ids": [
        909519
      ],
      "out_trade_no": "17740014820000561068",
      "route_name": "5号线-明珠-下班",
      "description": {
        "start_stop": "科创中心西门",
        "end_stop": "公交花园 ②",
        "date": [
          "2026-03-20 18:10:00"
        ]
      },
      "total": 3.0,
      "trade_state": "支付成功",
      "success_time": "2026-03-20 18:11:29",
      "timeout_time": "2026-03-20 18:11:23",
      "payer_total": 3.0
    },
    {
      "id": 558886,
      "coupon_ids": [],
      "price_total": 13.0,
      "ticket_ids": [
        905018
      ],
      "out_trade_no": "17739949120000558885",
      "route_name": "17号线-明珠线-上班",
      "description": {
        "start_stop": "长沙圩①",
        "end_stop": "科创中心西门",
        "date": [
          "2026-03-23 07:40:00"
        ]
      },
      "total": 3.0,
      "trade_state": "支付成功",
      "success_time": "2026-03-20 16:21:59",
      "timeout_time": "2026-03-20 16:21:53",
      "payer_total": 3.0
    },
    {
      "id": 556333,
      "coupon_ids": [],
      "price_total": 13.0,
      "ticket_ids": [
        899324
      ],
      "out_trade_no": "17739630490000556332",
      "route_name": "17号线-明珠线-上班",
      "description": {
        "start_stop": "长沙圩①",
        "end_stop": "科创中心西门",
        "date": [
          "2026-03-20 07:40:00"
        ]
      },
      "total": 3.0,
      "trade_state": "支付成功",
      "success_time": "2026-03-20 07:30:55",
      "timeout_time": "2026-03-20 07:30:50",
      "payer_total": 3.0
    },
    {
      "id": 554768,
      "coupon_ids": [],
      "price_total": 13.0,
      "ticket_ids": [
        897777
      ],
      "out_trade_no": "17739151980000554767",
      "route_name": "5号线-明珠-下班",
      "description": {
        "start_stop": "中医药产业园②",
        "end_stop": "公交花园 ②",
        "date": [
          "2026-03-19 18:10:00"
        ]
      },
      "total": 3.0,
      "trade_state": "支付成功",
      "success_time": "2026-03-19 18:13:25",
      "timeout_time": "2026-03-19 18:13:19",
      "payer_total": 3.0
    },
    {
      "id": 550825,
      "coupon_ids": [],
      "price_total": 13.0,
      "ticket_ids": [
        893802
      ],
      "out_trade_no": "17738322730000550824",
      "route_name": "5号线A-翠前",
      "description": {
        "start_stop": "中葡经贸中心(往口岸)",
        "end_stop": "华发新城 ②",
        "date": [
          "2026-03-18 19:10:00"
        ]
      },
      "total": 3.0,
      "trade_state": "支付成功",
      "success_time": "2026-03-18 19:11:19",
      "timeout_time": "2026-03-18 19:11:13",
      "payer_total": 3.0
    },
    {
      "id": 546644,
      "coupon_ids": [],
      "price_total": 13.0,
      "ticket_ids": [
        889521
      ],
      "out_trade_no": "17737512330000546643",
      "route_name": "2号线-前山",
      "description": {
        "start_stop": "厚朴道中(科创中心)②",
        "end_stop": "公交花园 ②",
        "date": [
          "2026-03-17 20:43:00"
        ]
      },
      "total": 3.0,
      "trade_state": "支付成功",
      "success_time": "2026-03-17 20:40:39",
      "timeout_time": "2026-03-17 20:40:33",
      "payer_total": 3.0
    },
    {
      "id": 541449,
      "coupon_ids": [],
      "price_total": 13.0,
      "ticket_ids": [
        883677
      ],
      "out_trade_no": "17736565790000541448",
      "route_name": "4号线-上冲/香洲-下班",
      "description": {
        "start_stop": "中葡经贸中心(往口岸)",
        "end_stop": "蓝盾路口②",
        "date": [
          "2026-03-16 18:25:00"
        ]
      },
      "total": 3.0,
      "trade_state": "支付成功",
      "success_time": "2026-03-16 18:23:07",
      "timeout_time": "2026-03-16 18:23:00",
      "payer_total": 3.0
    }
  ]
}
```
