#!/usr/bin/env bash
set -e

DIR="$1"
if [ -z "$DIR" ]; then
  echo "Usage: $0 <native-image-metadata-dir>"
  exit 1
fi

MERGED="$DIR/reachability-metadata.json"

# If the merged metadata already exists (e.g., from a prior non-native verify run), nothing to do
if [ -f "$MERGED" ]; then
  echo "[prepare-native-image-metadata] reachability-metadata.json already exists, skipping"
  exit 0
fi

# On macOS, bootstrap from the committed platform-specific metadata
if [ "$(uname)" = "Darwin" ] && [ -f "$DIR/reachability-metadata-macos.json" ]; then
  cp "$DIR/reachability-metadata-macos.json" "$MERGED"
  echo "[prepare-native-image-metadata] Copied reachability-metadata-macos.json -> reachability-metadata.json"
fi
