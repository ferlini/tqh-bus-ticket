# 路线站点

## Request URL
https://api.com/api/v1/route/timetable/stops

## Request Body
```
POST /api/v1/route/timetable/stops HTTP/1.1
Host: api.com
Connection: keep-alive
Content-Length: 39
X-Auth-Token: xxxxxx
content-type: application/json
Accept-Encoding: gzip,compress,br,deflate
User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 18_0_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.69(0x18004539) NetType/WIFI Language/zh_CN
Referer: https://servicewechat.com/wx98d802f3b76e9491/24/page-frame.html
```

## Request Body
```
{"route_id":275,"schedule_ids":[61429]}
```

## Response Body
```
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "up": [
      {
        "sequence": 0,
        "ti": 0,
        "time": "07:40",
        "version": "20251017-1",
        "stop_id": 861,
        "name": "诗僧路北②",
        "kind": "1",
        "x": "113.502968",
        "y": "22.27495",
        "is_optional": true,
        "message": null,
        "id": 1367197
      },
      {
        "sequence": 10,
        "ti": 3,
        "time": "07:43",
        "version": "20251017-1",
        "stop_id": 24,
        "name": "长沙圩①",
        "kind": "1",
        "x": "113.507772",
        "y": "22.274782",
        "is_optional": true,
        "message": null,
        "id": 1367198
      },
      {
        "sequence": 30,
        "ti": 3,
        "time": "07:46",
        "version": "20251017-1",
        "stop_id": 26,
        "name": "公交花园 ①",
        "kind": "1",
        "x": "113.512779",
        "y": "22.271025",
        "is_optional": true,
        "message": null,
        "id": 1367199
      },
      {
        "sequence": 50,
        "ti": 2,
        "time": "07:48",
        "version": "20251017-1",
        "stop_id": 747,
        "name": "翠微",
        "kind": "1",
        "x": "113.519547",
        "y": "22.257148",
        "is_optional": true,
        "message": null,
        "id": 1367200
      },
      {
        "sequence": 70,
        "ti": 3,
        "time": "07:51",
        "version": "20251017-1",
        "stop_id": 591,
        "name": "金嘉创意谷①",
        "kind": "1",
        "x": "113.51997326910262",
        "y": "22.253221171390223",
        "is_optional": true,
        "message": null,
        "id": 1367201
      },
      {
        "sequence": 75,
        "ti": 1,
        "time": "07:52",
        "version": "20251017-1",
        "stop_id": 749,
        "name": "明珠中",
        "kind": "1",
        "x": "113.520384",
        "y": "22.249397",
        "is_optional": true,
        "message": null,
        "id": 1367202
      },
      {
        "sequence": 90,
        "ti": 1,
        "time": "07:53",
        "version": "20251017-1",
        "stop_id": 645,
        "name": "明珠商业广场①",
        "kind": "1",
        "x": "113.520798",
        "y": "22.245567",
        "is_optional": true,
        "message": null,
        "id": 1367203
      },
      {
        "sequence": 100,
        "ti": 1,
        "time": "07:54",
        "version": "20251017-1",
        "stop_id": 258,
        "name": "漾湖明居 ①",
        "kind": "1",
        "x": "113.52190571392349",
        "y": "22.23822257833183",
        "is_optional": true,
        "message": null,
        "id": 1367204
      },
      {
        "sequence": 120,
        "ti": 4,
        "time": "07:58",
        "version": "20251017-1",
        "stop_id": 33,
        "name": "华发新城①",
        "kind": "1",
        "x": "113.519168",
        "y": "22.226852",
        "is_optional": true,
        "message": null,
        "id": 1367205
      },
      {
        "sequence": 130,
        "ti": 4,
        "time": "08:02",
        "version": "20251017-1",
        "stop_id": 91,
        "name": "南泉路口 ①",
        "kind": "1",
        "x": "113.502454",
        "y": "22.219728",
        "is_optional": true,
        "message": null,
        "id": 1367206
      },
      {
        "sequence": 140,
        "ti": 4,
        "time": "08:06",
        "version": "20251017-1",
        "stop_id": 493,
        "name": "广生 ①",
        "kind": "1",
        "x": "113.47145",
        "y": "22.21204",
        "is_optional": true,
        "message": null,
        "id": 1367207
      }
    ],
    "down": [
      {
        "sequence": 200,
        "ti": 24,
        "time": "08:30",
        "version": "20251017-1",
        "stop_id": 400,
        "name": "科创中心西门",
        "kind": "2",
        "x": "113.464749",
        "y": "22.130352",
        "is_optional": true,
        "message": null,
        "id": 1367208
      },
      {
        "sequence": 300,
        "ti": 2,
        "time": "08:32",
        "version": "20251017-1",
        "stop_id": 1118,
        "name": "厚朴道中（中医药产业园）",
        "kind": "2",
        "x": "113.463574",
        "y": "22.132466",
        "is_optional": true,
        "message": null,
        "id": 1367209
      },
      {
        "sequence": 1000,
        "ti": 11,
        "time": "08:43",
        "version": "20251017-1",
        "stop_id": 1115,
        "name": "市民中心(祥顺路东侧）（东1）",
        "kind": "2",
        "x": "113.520992",
        "y": "22.138284",
        "is_optional": true,
        "message": null,
        "id": 1367211
      }
    ],
    "pl": [],
    "route_id": 275,
    "route_name": "17号线-明珠线-上班",
    "label_id": 11,
    "label_name": "环岛北路线"
  }
}
```