#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:?APP_DIR nao definido}"
JAVA_CMD="${JAVA_CMD:-java}"
APP_JAR_NAME="${APP_JAR_NAME:-gestao.jar}"
APP_LOG_FILE="${APP_LOG_FILE:-app.log}"
APP_PID_FILE="${APP_PID_FILE:-app.pid}"
JAR_FILE="${JAR_FILE:?JAR_FILE nao definido}"
JVM_OPTS="${JVM_OPTS:-}"

mkdir -p "$APP_DIR"
TARGET_JAR="$APP_DIR/$APP_JAR_NAME"
PID_PATH="$APP_DIR/$APP_PID_FILE"
LOG_PATH="$APP_DIR/$APP_LOG_FILE"

stop_current_process() {
  if [ -f "$PID_PATH" ]; then
    PID=$(cat "$PID_PATH" || true)
    if [ -n "${PID:-}" ] && kill -0 "$PID" 2>/dev/null; then
      echo "Parando processo atual (PID: $PID)..."
      kill "$PID"
      for _ in {1..20}; do
        if kill -0 "$PID" 2>/dev/null; then
          sleep 1
        else
          break
        fi
      done
      if kill -0 "$PID" 2>/dev/null; then
        echo "Forcando parada do processo (PID: $PID)..."
        kill -9 "$PID"
      fi
    fi
  fi

  pkill -f "$TARGET_JAR" 2>/dev/null || true
}

echo "Publicando novo JAR em $TARGET_JAR"
mv "$JAR_FILE" "$TARGET_JAR"
chmod 644 "$TARGET_JAR"

stop_current_process

echo "Iniciando aplicacao..."
nohup "$JAVA_CMD" $JVM_OPTS -jar "$TARGET_JAR" >> "$LOG_PATH" 2>&1 &
NEW_PID=$!
echo "$NEW_PID" > "$PID_PATH"

echo "Aplicacao iniciada com PID: $NEW_PID"
