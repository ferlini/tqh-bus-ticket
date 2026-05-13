#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== 重启 tqh-bus-ticket ==="

"$SCRIPT_DIR/stop.sh"

# 等待 JVM 端口释放，避免随后 start 时端口仍被占用
sleep 2

"$SCRIPT_DIR/start.sh"
