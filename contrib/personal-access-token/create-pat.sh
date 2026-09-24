#!/usr/bin/env bash
# Creates a personal access token from a dashboard browser session. See README.md.
set -euo pipefail

cookie=$(cat "$1")
url=${2:-$(streamx settings get streamx.platform.url)}

response=$(curl -fsSk -X POST "${url%/}/api/v1/profile/tokens" \
  -H 'Content-Type: application/json' \
  -H "Cookie: ${cookie#Cookie: }" \
  --data "{\"name\":\"cli-$RANDOM\"}")

python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])' <<<"$response"
