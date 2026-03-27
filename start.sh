#!/bin/bash

APP_NAME="tqh-bus-ticket"
JAR_FILE="$HOME/tqh-bus-ticket/tqh-bus-ticket-0.0.1-SNAPSHOT.jar"
LOG_DIR="$HOME/tqh-bus-ticket/logs"
LOG_FILE="$LOG_DIR/prod.log"
PID_FILE="$LOG_DIR/$APP_NAME.pid"

mkdir -p "$LOG_DIR"

# 检查是否已在运行
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    if kill -0 "$OLD_PID" 2>/dev/null; then
        echo "$APP_NAME 已在运行 (PID: $OLD_PID)"
        exit 1
    fi
    rm -f "$PID_FILE"
fi

# 检查 JAR 是否存在
if [ ! -f "$JAR_FILE" ]; then
    echo "JAR 文件不存在: $JAR_FILE，正在构建..."
    mvn clean package -DskipTests -q
fi

echo "启动 $APP_NAME ..."
nohup java -jar "$JAR_FILE" \
    --spring.profiles.active=prod \
    --logging.file.name="$LOG_FILE" \
    > /dev/null 2>&1 &

echo $! > "$PID_FILE"
echo "$APP_NAME 已启动 (PID: $!)"
echo "日志文件: $LOG_FILE"
echo "查看日志: tail -f $LOG_FILE"
