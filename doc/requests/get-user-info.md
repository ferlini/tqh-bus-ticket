# 用户信息
## Request URL
https://api.com/api/v1/user/get/info

## Request Header
POST /api/v1/user/get/info HTTP/1.1
Host: api.com
Connection: keep-alive
Content-Length: 2
X-Auth-Token: xxxxxx
content-type: application/json
Accept-Encoding: gzip,compress,br,deflate
User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 18_0_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.69(0x18004539) NetType/WIFI Language/zh_CN
Referer: https://servicewechat.com/wx98d802f3b76e9491/24/page-frame.html

## Request Body
{}

## Response Body
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "full_name": "",
    "id_card": "",
    "id_type": "内地居民",
    "phone": "",
    "image": null,
    "user_type": "实名用户",
    "partner_id": null,
    "type_id": 1,
    "company_school_id": 0,
    "information_ids": null,
    "qualification_status": "审核成功"
  }
}