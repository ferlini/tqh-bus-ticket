#!/bin/bash
# 监听 ticket.log 变化，检测到新内容则输出

LOG_FILE="$HOME/tqh-bus-ticket/logs/ticket.log"
OFFSET_FILE="$HOME/tqh-bus-ticket/logs/.ticket-log-offset"

# 日志文件不存在
if [ ! -f "$LOG_FILE" ]; then
    exit 0
fi

CURRENT_SIZE=$(wc -c < "$LOG_FILE" | tr -d ' ')

# 首次运行，记录当前大小
if [ ! -f "$OFFSET_FILE" ]; then
    echo "$CURRENT_SIZE" > "$OFFSET_FILE"
    exit 0
fi

LAST_SIZE=$(cat "$OFFSET_FILE")

# 文件被清空（如每周五清理）
if [ "$CURRENT_SIZE" -lt "$LAST_SIZE" ]; then
    echo "$CURRENT_SIZE" > "$OFFSET_FILE"
    exit 0
fi

# 无新内容
if [ "$CURRENT_SIZE" -eq "$LAST_SIZE" ]; then
    exit 0
fi

# 有新内容，输出新增部分
echo "=== 购票日志有新内容 ==="
tail -c +"$((LAST_SIZE + 1))" "$LOG_FILE"
echo "$CURRENT_SIZE" > "$OFFSET_FILE"
