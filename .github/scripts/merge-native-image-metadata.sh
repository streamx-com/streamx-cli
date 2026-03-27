#!/usr/bin/env bash
set -e

OUTPUT_DIR="$1"
if [ -z "$OUTPUT_DIR" ]; then
  echo "Usage: $0 <output-dir>"
  exit 1
fi

INPUT_ARGS=""

# Include committed platform-specific metadata files
for f in "$OUTPUT_DIR"/reachability-metadata-*.json; do
  [ -f "$f" ] || continue
  dir=$(mktemp -d)
  cp "$f" "$dir/reachability-metadata.json"
  INPUT_ARGS="$INPUT_ARGS --input-dir=$dir"
done

# Include per-fork metadata from the tracing agent
FORK_DIRS=$(find "$OUTPUT_DIR" -maxdepth 1 -type d -name 'fork-*' 2>/dev/null | sort)
for dir in $FORK_DIRS; do
  INPUT_ARGS="$INPUT_ARGS --input-dir=$dir"
done

if [ -z "$INPUT_ARGS" ]; then
  exit 0
fi

echo "[native-image-configure] Merging native-image metadata into $OUTPUT_DIR"
"${JAVA_HOME}/bin/native-image-configure" generate $INPUT_ARGS --output-dir="$OUTPUT_DIR"

for dir in $FORK_DIRS; do
  rm -rf "$dir"
done

# On macOS, update the committed platform-specific metadata
if [ "$(uname)" = "Darwin" ]; then
  cp "$OUTPUT_DIR/reachability-metadata.json" "$OUTPUT_DIR/reachability-metadata-macos.json"
  echo "[native-image-configure] Updated reachability-metadata-macos.json"
fi
