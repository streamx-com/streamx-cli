#!/usr/bin/env bash
# Publishes the docs site into the gh-pages branch, one sub-directory per version.
#
#   deploy-docs.sh publish <dir>   copy every <dir>/<slot>/ into the branch, replacing what is there
#   deploy-docs.sh remove <slot>   delete <slot>/ (a closed pull request)
#
# Both rewrite the root index.html listing the published versions. Several runs (PRs finishing
# together) push to the same branch, so on a rejected push the commit is rebuilt on top of the
# new remote state and pushed again; a workflow-level lock would instead cancel the queued run
# of a different PR.
#
# Environment: GH_PAGES_REMOTE (push URL with credentials), GITHUB_REPOSITORY (links in the
# index), GITHUB_REF_NAME and GITHUB_SHA (commit message).
set -euo pipefail

ACTION="${1:-}"
TARGET="${2:-}"
if [[ "$ACTION" != publish && "$ACTION" != remove ]] || [[ -z "$TARGET" ]]; then
  echo "Usage: $0 publish <dir> | remove <slot>" >&2
  exit 1
fi
: "${GH_PAGES_REMOTE:?}" "${GITHUB_REPOSITORY:?}"

BRANCH="gh-pages"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# Versions are recognised by directory name. Anything else is a manual deployment.
RELEASE='^[0-9]+\.[0-9]+\.[0-9]+$'
PREVIEW='^[0-9]+\.[0-9]+\.[0-9]+-rc\.'
PR='^pr-[0-9]+$'
KNOWN='^([0-9]+\.[0-9]+\.[0-9]+(-rc\..*)?|pr-[0-9]+|latest|main)$'

apply_changes() {
  if [[ "$ACTION" == publish ]]; then
    for path in "$TARGET"/*/; do
      slot="$(basename "$path")"
      rm -rf "${WORK:?}/$slot"
      cp -R "${path%/}" "$WORK/$slot"
    done
  else
    rm -rf "${WORK:?}/$TARGET"
  fi
  touch "$WORK/.nojekyll"   # serve the Docusaurus output as-is, no Jekyll pass
  write_index > "$WORK/index.html"
}

# Published directories matching <regex>, one per line, in the given sort order.
slots() {  # <regex> [sort options...]
  local regex="$1"; shift
  (cd "$WORK" && find . -mindepth 1 -maxdepth 1 -type d ! -name .git | sed 's#^\./##') \
    | { grep -E "$regex" || true; } | sort "$@"
}

# One table row: the version/branch/PR label, its documentation, and optionally one link elsewhere.
row() {  # <label> <docs href> [<link text> <link href>]
  printf '<tr><td>%s</td><td><a href="%s">View documentation</a></td>' "$1" "$2"
  [[ $# -gt 2 ]] && printf '<td><a href="%s">%s</a></td>' "$4" "$3"
  printf '</tr>\n'
}

# A titled table around the rows on stdin. Prints nothing when there are none.
table() {  # <title> <first column header> [<third column header>] [<note html>]
  local rows
  rows="$(cat)"
  [[ -z "$rows" ]] && return 0
  printf '<h2>%s</h2>\n' "$1"
  [[ -n "${4:-}" ]] && printf '<p>%s</p>\n' "$4"
  printf '<table>\n<tr><th>%s</th><th>Documentation</th>' "$2"
  [[ -n "${3:-}" ]] && printf '<th>%s</th>' "$3"
  printf '</tr>\n%s\n</table>\n' "$rows"
}

write_index() {
  local gh="https://github.com/$GITHUB_REPOSITORY" assets latest note="" slot
  assets="$(cd "$(dirname "$0")/../.." && pwd)/docs/static/img"
  latest="$(slots "$RELEASE" -rV | head -n1)"
  if [[ -n "$latest" && -d "$WORK/latest" ]]; then
    note="The newest release is always available at <a href=\"latest/\">latest</a>."
  fi

  cat <<EOF
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>StreamX CLI documentation</title>
EOF
  if [[ -f "$assets/favicon.svg" ]]; then
    printf '<link rel="icon" href="data:image/svg+xml;base64,%s">\n' "$(base64 < "$assets/favicon.svg" | tr -d '\n')"
  fi
  cat <<EOF
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;600&display=swap">
<style>
  /* Same ground, text, accent and typeface as the docs site (docs/src/css/custom.css). */
  body { max-width: 56rem; margin: 2rem auto; padding: 0 1rem; background: #ffffff; color: #0a0a0b;
         font-family: "Be Vietnam Pro", system-ui, sans-serif; line-height: 1.5; }
  a { color: #7714ff; }
  table { width: 100%; border-collapse: collapse; margin-bottom: 2rem; }
  th, td { text-align: left; padding: 0.4rem 0.75rem; border-bottom: 1px solid #e6e6e6; }
</style>
</head>
<body>
EOF
  [[ -f "$assets/streamx-logo-light-bg.svg" ]] && cat "$assets/streamx-logo-light-bg.svg" && echo
  cat <<EOF
<h1>CLI documentation</h1>
<p>Every published version of the command reference and guides.</p>
EOF

  slots "$RELEASE" -rV | while IFS= read -r slot; do
    row "$slot" "$slot/" "View release notes" "$gh/releases/tag/$slot"
  done | table "Releases" "Version" "Release notes" "$note"

  # Preview releases have no release notes.
  slots "$PREVIEW" -rV | while IFS= read -r slot; do
    row "$slot" "$slot/"
  done | table "Preview releases" "Version"

  slots "$PR" -t- -k2 -rn | while IFS= read -r slot; do
    row "#${slot#pr-}" "$slot/" "View PR on GitHub" "$gh/pull/${slot#pr-}"
  done | table "Pull requests" "Pull request" "GitHub"

  { [[ -d "$WORK/main" ]] && echo main; slots '.' | { grep -Ev "$KNOWN" || true; }; } | while IFS= read -r slot; do
    [[ -z "$slot" ]] && continue
    row "$slot" "$slot/" "View branch on GitHub" "$gh/tree/$slot"
  done | table "Branches" "Branch" "GitHub"

  cat <<EOF
</body>
</html>
EOF
}

if [[ "$ACTION" == publish ]]; then
  MESSAGE="Publish $(basename -a "$TARGET"/*/ | tr '\n' ' ')from ${GITHUB_REF_NAME:-?} (${GITHUB_SHA:-?})"
else
  MESSAGE="Remove $TARGET (${GITHUB_REF_NAME:-?})"
fi

for attempt in $(seq 1 10); do
  rm -rf "$WORK" && mkdir -p "$WORK"
  if git ls-remote --exit-code --heads "$GH_PAGES_REMOTE" "$BRANCH" > /dev/null 2>&1; then
    git clone -q --depth 1 --branch "$BRANCH" "$GH_PAGES_REMOTE" "$WORK"
  else
    git -C "$WORK" init -q -b "$BRANCH"
    git -C "$WORK" remote add origin "$GH_PAGES_REMOTE"
  fi

  apply_changes
  git -C "$WORK" add -A
  if git -C "$WORK" diff --cached --quiet; then
    echo "Nothing to publish."
    exit 0
  fi
  git -C "$WORK" \
    -c user.name='github-actions[bot]' \
    -c user.email='41898282+github-actions[bot]@users.noreply.github.com' \
    commit -q -m "$MESSAGE"
  if git -C "$WORK" push -q origin "$BRANCH"; then
    echo "$MESSAGE"
    exit 0
  fi
  echo "Push rejected (attempt $attempt), retrying on top of the new remote state."
  sleep $((RANDOM % 8 + 2))
done

echo "Could not push to $BRANCH after 10 attempts." >&2
exit 1
