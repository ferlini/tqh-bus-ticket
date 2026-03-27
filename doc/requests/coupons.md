# 查询可用的优惠券

## 请求URL
```
https://api.com/api/v2/order/coupon
```

## Request Header
```
POST /api/v2/order/coupon HTTP/1.1
Host: api.com
Connection: keep-alive
Content-Length: 62
X-Auth-Token: xxxxxx
content-type: application/json
Accept-Encoding: gzip,compress,br,deflate
User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 18_0_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.69(0x18004539) NetType/WIFI Language/zh_CN
Referer: https://servicewechat.com/wx98d802f3b76e9491/24/page-frame.html
```

## Request Body
```
{"route_id":275,"schedule_ids":[61429],"boarding_point_id":24}
```

## Response Body Example
```
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 8317178,
      "activity_id": 5,
      "coupon_name": "粤澳合作区政府券",
      "due_datetime": "2026-03-27 00:00:00",
      "is_use": {
        "61429": true
      },
      "denomination": 10.0,
      "status": {
        "61429": "待使用"
      },
      "create_date": "2026-03-20 00:00:18",
      "msy": {
        "61429": null
      },
      "coupon_category_id": 2,
      "coupon_category_name": "政府券"
    },
    {
      "id": 8317179,
      "activity_id": 5,
      "coupon_name": "粤澳合作区政府券",
      "due_datetime": "2026-03-27 00:00:00",
      "is_use": {
        "61429": true
      },
      "denomination": 10.0,
      "status": {
        "61429": "待使用"
      },
      "create_date": "2026-03-20 00:00:18",
      "msy": {
        "61429": null
      },
      "coupon_category_id": 2,
      "coupon_category_name": "政府券"
    },
    {
      "id": 8317180,
      "activity_id": 5,
      "coupon_name": "粤澳合作区政府券",
      "due_datetime": "2026-03-27 00:00:00",
      "is_use": {
        "61429": true
      },
      "denomination": 10.0,
      "status": {
        "61429": "待使用"
      },
      "create_date": "2026-03-20 00:00:18",
      "msy": {
        "61429": null
      },
      "coupon_category_id": 2,
      "coupon_category_name": "政府券"
    },
    {
      "id": 8317181,
      "activity_id": 5,
      "coupon_name": "粤澳合作区政府券",
      "due_datetime": "2026-03-27 00:00:00",
      "is_use": {
        "61429": true
      },
      "denomination": 10.0,
      "status": {
        "61429": "待使用"
      },
      "create_date": "2026-03-20 00:00:18",
      "msy": {
        "61429": null
      },
      "coupon_category_id": 2,
      "coupon_category_name": "政府券"
    },
    {
      "id": 8317182,
      "activity_id": 5,
      "coupon_name": "粤澳合作区政府券",
      "due_datetime": "2026-03-27 00:00:00",
      "is_use": {
        "61429": true
      },
      "denomination": 10.0,
      "status": {
        "61429": "待使用"
      },
      "create_date": "2026-03-20 00:00:18",
      "msy": {
        "61429": null
      },
      "coupon_category_id": 2,
      "coupon_category_name": "政府券"
    },
    {
      "id": 8317183,
      "activity_id": 5,
      "coupon_name": "粤澳合作区政府券",
      "due_datetime": "2026-03-27 00:00:00",
      "is_use": {
        "61429": true
      },
      "denomination": 10.0,
      "status": {
        "61429": "待使用"
      },
      "create_date": "2026-03-20 00:00:18",
      "msy": {
        "61429": null
      },
      "coupon_category_id": 2,
      "coupon_category_name": "政府券"
    },
    {
      "id": 8317184,
      "activity_id": 5,
      "coupon_name": "粤澳合作区政府券",
      "due_datetime": "2026-03-27 00:00:00",
      "is_use": {
        "61429": true
      },
      "denomination": 10.0,
      "status": {
        "61429": "待使用"
      },
      "create_date": "2026-03-20 00:00:18",
      "msy": {
        "61429": null
      },
      "coupon_category_id": 2,
      "coupon_category_name": "政府券"
    },
    {
      "id": 8317185,
      "activity_id": 5,
      "coupon_name": "粤澳合作区政府券",
      "due_datetime": "2026-03-27 00:00:00",
      "is_use": {
        "61429": true
      },
      "denomination": 10.0,
      "status": {
        "61429": "待使用"
      },
      "create_date": "2026-03-20 00:00:18",
      "msy": {
        "61429": null
      },
      "coupon_category_id": 2,
      "coupon_category_name": "政府券"
    },
    {
      "id": 8317186,
      "activity_id": 5,
      "coupon_name": "粤澳合作区政府券",
      "due_datetime": "2026-03-27 00:00:00",
      "is_use": {
        "61429": true
      },
      "denomination": 10.0,
      "status": {
        "61429": "待使用"
      },
      "create_date": "2026-03-20 00:00:18",
      "msy": {
        "61429": null
      },
      "coupon_category_id": 2,
      "coupon_category_name": "政府券"
    },
    {
      "id": 8317187,
      "activity_id": 5,
      "coupon_name": "粤澳合作区政府券",
      "due_datetime": "2026-03-27 00:00:00",
      "is_use": {
        "61429": true
      },
      "denomination": 10.0,
      "status": {
        "61429": "待使用"
      },
      "create_date": "2026-03-20 00:00:18",
      "msy": {
        "61429": null
      },
      "coupon_category_id": 2,
      "coupon_category_name": "政府券"
    },
    {
      "id": 8317188,
      "activity_id": 5,
      "coupon_name": "粤澳合作区政府券",
      "due_datetime": "2026-03-27 00:00:00",
      "is_use": {
        "61429": true
      },
      "denomination": 10.0,
      "status": {
        "61429": "待使用"
      },
      "create_date": "2026-03-20 00:00:18",
      "msy": {
        "61429": null
      },
      "coupon_category_id": 2,
      "coupon_category_name": "政府券"
    },
    {
      "id": 8317189,
      "activity_id": 5,
      "coupon_name": "粤澳合作区政府券",
      "due_datetime": "2026-03-27 00:00:00",
      "is_use": {
        "61429": true
      },
      "denomination": 10.0,
      "status": {
        "61429": "待使用"
      },
      "create_date": "2026-03-20 00:00:18",
      "msy": {
        "61429": null
      },
      "coupon_category_id": 2,
      "coupon_category_name": "政府券"
    },
    {
      "id": 8190582,
      "activity_id": 8,
      "coupon_name": "粤澳合作区政府券",
      "due_datetime": "2026-03-27 00:00:00",
      "is_use": {
        "61429": false
      },
      "denomination": 23.0,
      "status": {
        "61429": "不可使用"
      },
      "create_date": "2026-03-20 00:00:35",
      "msy": {
        "61429": "该线路不支持用此券"
      },
      "coupon_category_id": 2,
      "coupon_category_name": "政府券"
    },
    {
      "id": 8190583,
      "activity_id": 8,
      "coupon_name": "粤澳合作区政府券",
      "due_datetime": "2026-03-27 00:00:00",
      "is_use": {
        "61429": false
      },
      "denomination": 23.0,
      "status": {
        "61429": "不可使用"
      },
      "create_date": "2026-03-20 00:00:35",
      "msy": {
        "61429": "该线路不支持用此券"
      },
      "coupon_category_id": 2,
      "coupon_category_name": "政府券"
    },
    {
      "id": 8190584,
      "activity_id": 8,
      "coupon_name": "粤澳合作区政府券",
      "due_datetime": "2026-03-27 00:00:00",
      "is_use": {
        "61429": false
      },
      "denomination": 23.0,
      "status": {
        "61429": "不可使用"
      },
      "create_date": "2026-03-20 00:00:35",
      "msy": {
        "61429": "该线路不支持用此券"
      },
      "coupon_category_id": 2,
      "coupon_category_name": "政府券"
    },
    {
      "id": 8190585,
      "activity_id": 8,
      "coupon_name": "粤澳合作区政府券",
      "due_datetime": "2026-03-27 00:00:00",
      "is_use": {
        "61429": false
      },
      "denomination": 23.0,
      "status": {
        "61429": "不可使用"
      },
      "create_date": "2026-03-20 00:00:35",
      "msy": {
        "61429": "该线路不支持用此券"
      },
      "coupon_category_id": 2,
      "coupon_category_name": "政府券"
    },
    {
      "id": 8190586,
      "activity_id": 8,
      "coupon_name": "粤澳合作区政府券",
      "due_datetime": "2026-03-27 00:00:00",
      "is_use": {
        "61429": false
      },
      "denomination": 23.0,
      "status": {
        "61429": "不可使用"
      },
      "create_date": "2026-03-20 00:00:35",
      "msy": {
        "61429": "该线路不支持用此券"
      },
      "coupon_category_id": 2,
      "coupon_category_name": "政府券"
    },
    {
      "id": 8190587,
      "activity_id": 8,
      "coupon_name": "粤澳合作区政府券",
      "due_datetime": "2026-03-27 00:00:00",
      "is_use": {
        "61429": false
      },
      "denomination": 23.0,
      "status": {
        "61429": "不可使用"
      },
      "create_date": "2026-03-20 00:00:35",
      "msy": {
        "61429": "该线路不支持用此券"
      },
      "coupon_category_id": 2,
      "coupon_category_name": "政府券"
    },
    {
      "id": 8190588,
      "activity_id": 8,
      "coupon_name": "粤澳合作区政府券",
      "due_datetime": "2026-03-27 00:00:00",
      "is_use": {
        "61429": false
      },
      "denomination": 23.0,
      "status": {
        "61429": "不可使用"
      },
      "create_date": "2026-03-20 00:00:35",
      "msy": {
        "61429": "该线路不支持用此券"
      },
      "coupon_category_id": 2,
      "coupon_category_name": "政府券"
    },
    {
      "id": 8190589,
      "activity_id": 8,
      "coupon_name": "粤澳合作区政府券",
      "due_datetime": "2026-03-27 00:00:00",
      "is_use": {
        "61429": false
      },
      "denomination": 23.0,
      "status": {
        "61429": "不可使用"
      },
      "create_date": "2026-03-20 00:00:35",
      "msy": {
        "61429": "该线路不支持用此券"
      },
      "coupon_category_id": 2,
      "coupon_category_name": "政府券"
    },
    {
      "id": 8190590,
      "activity_id": 8,
      "coupon_name": "粤澳合作区政府券",
      "due_datetime": "2026-03-27 00:00:00",
      "is_use": {
        "61429": false
      },
      "denomination": 23.0,
      "status": {
        "61429": "不可使用"
      },
      "create_date": "2026-03-20 00:00:35",
      "msy": {
        "61429": "该线路不支持用此券"
      },
      "coupon_category_id": 2,
      "coupon_category_name": "政府券"
    },
    {
      "id": 8190591,
      "activity_id": 8,
      "coupon_name": "粤澳合作区政府券",
      "due_datetime": "2026-03-27 00:00:00",
      "is_use": {
        "61429": false
      },
      "denomination": 23.0,
      "status": {
        "61429": "不可使用"
      },
      "create_date": "2026-03-20 00:00:35",
      "msy": {
        "61429": "该线路不支持用此券"
      },
      "coupon_category_id": 2,
      "coupon_category_name": "政府券"
    }
  ]
}
```