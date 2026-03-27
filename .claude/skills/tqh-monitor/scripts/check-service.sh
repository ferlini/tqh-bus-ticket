#!/bin/bash
# 检查通勤号监控服务状态，未启动则自动启动

PORT=8080
APP_DIR="$HOME/tqh-bus-ticket"
JAR_FILE="$APP_DIR/tqh-bus-ticket-0.0.1-SNAPSHOT.jar"
LOG_DIR="$APP_DIR/logs"
PID_FILE="$LOG_DIR/tqh-bus-ticket.pid"
LOG_FILE="$LOG_DIR/prod.log"

PID=$(lsof -t -i :$PORT 2>/dev/null)

if [ -n "$PID" ]; then
    echo "RUNNING|$PID"
    exit 0
fi

echo "NOT_RUNNING"

if [ ! -f "$JAR_FILE" ]; then
    echo "JAR_NOT_FOUND|$JAR_FILE"
    exit 1
fi

mkdir -p "$LOG_DIR"

echo "STARTING"
nohup java -jar "$JAR_FILE" \
    --spring.profiles.active=prod \
    --logging.file.name="$LOG_FILE" \
    > /dev/null 2>&1 &
NEW_PID=$!
echo $NEW_PID > "$PID_FILE"

# 等待服务就绪（最多 30 秒）
for i in $(seq 1 30); do
    if lsof -i :$PORT > /dev/null 2>&1; then
        echo "STARTED|$NEW_PID"
        exit 0
    fi
    sleep 1
done

echo "START_TIMEOUT"
exit 1
