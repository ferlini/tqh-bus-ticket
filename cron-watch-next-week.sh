#!/bin/bash
# 每周五中午自动启动「监控下周车票」watch 模式。
# Watch 模式发现任意目标日期有票后，会发送 OpenClaw Webhook 并自动停止本次监控。
# 监控启动成功后，再向用户微信发送一条「正在为您监控下周车票」开始通知（与有票通知不同的 channel）。
#
# 敏感配置（OpenClaw URL/Token/Channel）从 ~/tqh-bus-ticket/.env 读取，模板见 .env.example。

set -u

SKILL_DIR="$HOME/.openclaw/skills/tqh-monitor"
ENV_FILE="$HOME/tqh-bus-ticket/.env"
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

echo "[$TIMESTAMP] === cron: 启动监控下周车票 ==="

if [ -x "$SKILL_DIR/scripts/check-service.sh" ]; then
    bash "$SKILL_DIR/scripts/check-service.sh"
else
    echo "[$TIMESTAMP] check-service.sh 不存在，跳过服务自检"
fi

if curl --fail -sS -X POST http://localhost:8080/monitor/watch/next-week; then
    echo
    if [ -f "$ENV_FILE" ]; then
        # shellcheck disable=SC1090
        set -a; . "$ENV_FILE"; set +a
    fi
    if [ -n "${OPENCLAW_URL:-}" ] && [ -n "${OPENCLAW_TOKEN:-}" ] && [ -n "${OPENCLAW_START_CHANNEL:-}" ]; then
        echo "[$TIMESTAMP] 监控启动成功，发送开始通知到 $OPENCLAW_START_CHANNEL"
        START_NOTIFY_TEXT="给${OPENCLAW_START_CHANNEL} 发送并美化内容：正在为您监控下周车票，有票立即通知您[爱心]"
        PAYLOAD=$(printf '{"text":"%s","mode":"now"}' "$START_NOTIFY_TEXT")
        curl -sS -X POST "$OPENCLAW_URL" \
            -H "Authorization: Bearer $OPENCLAW_TOKEN" \
            -H 'Content-Type: application/json' \
            -d "$PAYLOAD"
        echo
    else
        echo "[$TIMESTAMP] 缺少 OpenClaw 配置（$ENV_FILE 不存在或变量未设置），跳过开始通知"
    fi
else
    echo
    echo "[$TIMESTAMP] 监控启动失败，跳过开始通知"
fi

echo "[$TIMESTAMP] === cron: 完成 ==="
