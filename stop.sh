#!/bin/bash

APP_NAME="tqh-bus-ticket"
PID_FILE="$HOME/tqh-bus-ticket/logs/$APP_NAME.pid"

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
