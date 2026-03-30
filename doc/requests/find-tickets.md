# 查找车票

## Request URL
https://api.com/api/v1/route/ticket/find

## Request Header
```
POST /api/v1/route/ticket/find HTTP/1.1
Host: api.com
Connection: keep-alive
Content-Length: 36
X-Auth-Token: xxxxxx
content-type: application/json
Accept-Encoding: gzip,compress,br,deflate
User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 26_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.70(0x18004624) NetType/WIFI Language/zh_CN
Referer: https://servicewechat.com/wx98d802f3b76e9491/24/page-frame.html
```

## Request Body
```
{"status":"NOW","page":1,"limit":10}
```

## Response Body
```
{"code":200,"msg":"查询成功","data":[{"id":932762,"route_id":275,"route":"17号线-明珠线-上班","labels":["环岛北路线"],"start_stop":"长沙圩①","end_stop":"科创中心西门","time_departure":["2026-03-24","07:40:00"],"time_departure_str":"2026-03-24 07:40:00","time_expected_arrival":["2026-03-24","07:43:00"],"status":"已使用","is_refund":false,"is_rebook":false,"bus_id":"KMQNvF86hqDwKosS3b2+tQ%3D%3D","labels_types":["跨区线"],"is_within_departure_time":false,"abnormal_info":{"abnormal_state":null,"abnormal_info":null},"refund_amount":0,"refunding_amount":0,"refundable_amount":300,"plate_number":"粤C07189D"}]}
```
