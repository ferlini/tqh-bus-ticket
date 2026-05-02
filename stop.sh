#!/bin/bash

APP_NAME="tqh-bus-ticket"
PID_FILE="$HOME/tqh-bus-ticket/logs/$APP_NAME.pid"
CRON_TAG="# tqh-bus-ticket: watch-next-week"

# 卸载 cronjob：移除 start.sh 安装的「每周五监控下周车票」定时任务。
# 服务停止后保留该 cron 没意义：触发时只会得到 connection refused。
if crontab -l 2>/dev/null | grep -qF "$CRON_TAG"; then
    crontab -l 2>/dev/null | grep -vF "$CRON_TAG" | crontab -
    echo "已卸载 cronjob"
fi

if [ ! -f "$PID_FILE" ]; then
    echo "$APP_NAME 未在运行"
    exit 0
fi

PID=$(cat "$PID_FILE")
if kill -0 "$PID" 2>/dev/null; then
    kill "$PID"
    rm -f "$PID_FILE"
    echo "$APP_NAME 已停止 (PID: $PID)"
else
    rm -f "$PID_FILE"
    echo "$APP_NAME 进程不存在，已清理 PID 文件"
fi
