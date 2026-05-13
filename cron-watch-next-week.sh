#!/bin/bash
# 每周五中午自动触发「购买下周车票」（purchase 模式，发现有票即合并下单并停止监控）。
# 服务启动成功后，再通过 OpenClaw agent webhook 向用户发送一条「开始购票」通知。
#
# 敏感配置（OpenClaw URL/Token/Agent/Channel/Targets）从 ~/tqh-bus-ticket/.env 读取，
# 模板见 .env.example。

set -u

SKILL_DIR="$HOME/.openclaw/skills/tqh-monitor"
ENV_FILE="$HOME/tqh-bus-ticket/.env"
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

echo "[$TIMESTAMP] === cron: 启动购买下周车票 ==="

if [ -x "$SKILL_DIR/scripts/check-service.sh" ]; then
    bash "$SKILL_DIR/scripts/check-service.sh"
else
    echo "[$TIMESTAMP] check-service.sh 不存在，跳过服务自检"
fi

if curl --fail -sS -X POST http://localhost:8080/monitor/next-week; then
    echo
    if [ -f "$ENV_FILE" ]; then
        # shellcheck disable=SC1090
        set -a; . "$ENV_FILE"; set +a
    fi
    if [ -n "${OPENCLAW_URL:-}" ] \
        && [ -n "${OPENCLAW_TOKEN:-}" ] \
        && [ -n "${OPENCLAW_AGENT_NAME:-}" ] \
        && [ -n "${OPENCLAW_CHANNEL:-}" ] \
        && [ -n "${OPENCLAW_TARGETS:-}" ]; then
        echo "[$TIMESTAMP] 监控启动成功，通过 OpenClaw agent 发送开始购票通知 (channel=$OPENCLAW_CHANNEL)"
        START_NOTIFY_TEXT="给 ${OPENCLAW_TARGETS} 发送并美化内容：正在为您购买下周车票，购票后立即通知您[爱心]"
        PAYLOAD=$(printf '{"message":"%s","name":"%s","channel":"%s"}' \
            "$START_NOTIFY_TEXT" "$OPENCLAW_AGENT_NAME" "$OPENCLAW_CHANNEL")
        curl -sS -X POST "$OPENCLAW_URL" \
            -H "Authorization: Bearer $OPENCLAW_TOKEN" \
            -H 'Content-Type: application/json' \
            -d "$PAYLOAD"
        echo
    else
        echo "[$TIMESTAMP] 缺少 OpenClaw 配置（$ENV_FILE 不存在或 OPENCLAW_URL/TOKEN/AGENT_NAME/CHANNEL/TARGETS 任一未设置），跳过开始通知"
    fi
else
    echo
    echo "[$TIMESTAMP] 监控启动失败，跳过开始通知"
fi

echo "[$TIMESTAMP] === cron: 完成 ==="
