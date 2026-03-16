#!/usr/bin/env bash
set -euo pipefail

if ! command -v docker &> /dev/null; then
  echo "Docker not found, skipping docker log collection."
  exec "$@"
fi

LOGS_DIR="${DOCKER_LOGS_DIR:-target/docker-logs}"
mkdir -p "$LOGS_DIR"

EVENTS_LOG="${LOGS_DIR}/docker-events.log"

log_event() {
  echo "[$(date -u '+%Y-%m-%dT%H:%M:%SZ')] $*" | tee -a "$EVENTS_LOG"
}

# Named pipes so we can capture PIDs of both sides of each pipeline
IMAGE_FIFO=$(mktemp -u)
CONTAINER_FIFO=$(mktemp -u)
mkfifo "$IMAGE_FIFO" "$CONTAINER_FIFO"

# --- Image pull logging ---
docker events \
  --filter 'type=image' \
  --format '{{.Time}} IMAGE {{.Action}} {{.Actor.Attributes.name}}' \
  > "$IMAGE_FIFO" &
IMAGE_EVENTS_PID=$!

while read -r line; do
  log_event "$line"
done < "$IMAGE_FIFO" &
IMAGE_READER_PID=$!

# --- Container lifecycle logging ---
docker events \
  --filter 'type=container' \
  --format '{{.Time}} CONTAINER {{.Action}} name={{.Actor.Attributes.name}} id={{.ID}} image={{.Actor.Attributes.image}}' \
  > "$CONTAINER_FIFO" &
CONTAINER_EVENTS_PID=$!

while read -r event; do
  log_event "$event"

  action=$(echo "$event" | awk '{print $3}')
  name=$(echo   "$event" | awk '{print $4}' | cut -d= -f2)
  id=$(echo     "$event" | awk '{print $5}' | cut -d= -f2)
  short_id="${id:0:12}"
  logfile="${LOGS_DIR}/${name}-${short_id}.log"

  case "$action" in
    create)
      log_event "Container created: $name ($short_id) — capturing inspect snapshot"
      {
        echo "=== docker inspect at CREATE ($(date -u '+%Y-%m-%dT%H:%M:%SZ')) ==="
        docker inspect "$id" 2>&1
        echo ""
      } >> "$logfile"
      ;;
    start)
      log_event "Container started: $name ($short_id) — following logs"
      {
        echo "=== docker inspect at START ($(date -u '+%Y-%m-%dT%H:%M:%SZ')) ==="
        docker inspect "$id" 2>&1
        echo ""
        echo "=== Container logs ==="
      } >> "$logfile"
      docker logs -f "$id" >> "$logfile" 2>&1 &
      ;;
    die|stop|kill|oom)
      log_event "Container stopped ($action): $name ($short_id) — capturing exit state"
      {
        echo "=== docker inspect at ${action^^} ($(date -u '+%Y-%m-%dT%H:%M:%SZ')) ==="
        docker inspect "$id" 2>&1 || true
        echo ""
      } >> "$logfile"
      ;;
  esac
done < "$CONTAINER_FIFO" &
CONTAINER_READER_PID=$!

cleanup() {
  log_event "Shutting down docker log collector"
  kill "$IMAGE_EVENTS_PID" "$IMAGE_READER_PID" \
       "$CONTAINER_EVENTS_PID" "$CONTAINER_READER_PID" 2>/dev/null || true
  pkill -f "docker logs -f" 2>/dev/null || true
  rm -f "$IMAGE_FIFO" "$CONTAINER_FIFO"
  wait "$IMAGE_EVENTS_PID" "$IMAGE_READER_PID" \
       "$CONTAINER_EVENTS_PID" "$CONTAINER_READER_PID" 2>/dev/null || true
}
trap cleanup EXIT

log_event "Docker log collector started (PID $$), writing to $LOGS_DIR"

"$@"