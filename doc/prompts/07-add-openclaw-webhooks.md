TicketMonitorService.processSchedule() 在购票成功后，除了写日志，还需要调用 OpenClaw 的 WebHooks，给微信的clawbot channel "openclaw-weixin"发送下单成功的消息，webhook 的设置要求写在配置文件中。
1. 消息发送使用http post 请求，content_type 是 application/json
2. webhook 的 uri 是 http://127.0.0.1:18789/hooks/wake
3. 请求头 Authorization 中填写 token。例如：Authorization: Bearer webhook token
4. 消息 channel：openclaw-weixin b263ba753a2e-im-bot 
5. 消息体的格式为 {"text":"给 openclaw-weixin b263ba753a2e-im-bot 发送内容：老婆，中午好~","mode":"now"} 
6. Webhook 推送内容：“给 <消息channel> 发送：<购票成功消息>”，购票成功消息与购票日志相同。