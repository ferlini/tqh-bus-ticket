# 查询车次库存

## Request URL
```
https://api.com/api/v1/route/schedule/find
```

## Request Header
```
POST /api/v1/route/schedule/find HTTP/1.1
Host: api.com
Connection: keep-alive
Content-Length: 16
X-Auth-Token: xxxxxx
content-type: application/json
Accept-Encoding: gzip,compress,br,deflate
User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 18_0_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.69(0x18004539) NetType/WIFI Language/zh_CN
Referer: https://servicewechat.com/wx98d802f3b76e9491/24/page-frame.html
```

## Request Body
```
{"route_id":275}
```

## Response Body
```
{
  "code": 200,
  "msg": "查询成功",
  "data": {
    "07:40": [
      {
        "id": 61432,
        "date": "2026/3/27",
        "time": "07:40",
        "price": 13.0,
        "number": 0,
        "abnormal_info": {
          "abnormal_state": null,
          "abnormal_info": null
        }
      },
      {
        "id": 61431,
        "date": "2026/3/26",
        "time": "07:40",
        "price": 13.0,
        "number": 0,
        "abnormal_info": {
          "abnormal_state": null,
          "abnormal_info": null
        }
      },
      {
        "id": 61430,
        "date": "2026/3/25",
        "time": "07:40",
        "price": 13.0,
        "number": 0,
        "abnormal_info": {
          "abnormal_state": null,
          "abnormal_info": null
        }
      },
      {
        "id": 61429,
        "date": "2026/3/24",
        "time": "07:40",
        "price": 13.0,
        "number": 1,
        "abnormal_info": {
          "abnormal_state": null,
          "abnormal_info": null
        }
      }
    ]
  }
}
```