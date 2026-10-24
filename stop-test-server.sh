#!/bin/bash
# CoreServerMod Test Server Stop Script
# Safely stops the test server

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="$SCRIPT_DIR/test-server"
PID_FILE="$SERVER_DIR/server.pid"

echo "=== CoreServerMod Test Server Stop ==="

# Try to find server process
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE" 2>/dev/null)
    if [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; then
        echo "Stopping server (PID: $PID)..."
        kill -TERM "$PID"
        
        # Wait for graceful shutdown
        for i in {1..30}; do
            if ! kill -0 "$PID" 2>/dev/null; then
                echo "Server stopped gracefully."
                rm -f "$PID_FILE"
                exit 0
            fi
            sleep 1
        done
        
        # Force kill if still running
        echo "Server did not stop gracefully, forcing..."
        kill -KILL "$PID" 2>/dev/null
        sleep 2
        rm -f "$PID_FILE"
        echo "Server forcefully stopped."
        exit 0
    fi
fi

# Fallback: find by port
PID=$(lsof -ti:25565 2>/dev/null | head -1)
if [ -n "$PID" ]; then
    echo "Found server on port 25565 (PID: $PID)"
    kill -TERM "$PID"
    sleep 5
    if kill -0 "$PID" 2>/dev/null; then
        kill -KILL "$PID"
    fi
    echo "Server stopped."
    exit 0
fi

echo "No running server found."
exit 0