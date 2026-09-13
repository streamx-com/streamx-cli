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

BRANCH=gh-pages
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# Versions are recognised by directory name; anything else is a manual deployment.
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

li() {  # <slot> <label> [<extra html>]
  printf '  <li><a href="%s/">%s</a>%s</li>\n' "$1" "$2" "${3:+ $3}"
}

lis() {  # slots on stdin -> one <li> each; PR entries also link to the pull request
  local slot
  while IFS= read -r slot; do
    [[ -z "$slot" ]] && continue
    if [[ "$slot" =~ $PR ]]; then
      li "$slot" "$slot" "<a class=\"muted\" href=\"https://github.com/$GITHUB_REPOSITORY/pull/${slot#pr-}\">#${slot#pr-}</a>"
    else
      li "$slot" "$slot"
    fi
  done
}

section() {  # <title>; <li> lines on stdin; prints nothing for an empty list
  local body
  body="$(cat)"
  [[ -z "$body" ]] && return 0
  printf '<h2>%s</h2>\n<ul>\n%s\n</ul>\n' "$1" "$body"
}

write_index() {
  local latest
  latest="$(slots "$RELEASE" -rV | head -n1)"
  cat <<EOF
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>StreamX CLI docs</title>
<style>
  body { margin: 0; padding: 3rem 1.5rem; background: #0a0a0b; color: #ecf5ff;
         font: 16px/1.6 -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif; }
  main { max-width: 40rem; margin: 0 auto; }
  h1 { font-size: 1.75rem; margin: 0 0 0.25rem; }
  h2 { font-size: 0.8125rem; margin: 2rem 0 0.5rem; color: #9aa4b2; text-transform: uppercase; letter-spacing: 0.08em; }
  ul { list-style: none; margin: 0; padding: 0; }
  li { padding: 0.4rem 0; border-bottom: 1px solid #232323; }
  a { color: #b98bff; text-decoration: none; }
  a:hover { text-decoration: underline; }
  .lead, .muted { color: #9aa4b2; }
  .muted { font-size: 0.875rem; margin-left: 0.5rem; }
</style>
</head>
<body>
<main>
<h1>StreamX CLI documentation</h1>
<p class="lead">Every published version of the command reference and guides.</p>
EOF
  if [[ -n "$latest" && -d "$WORK/latest" ]]; then
    li latest "$latest" | section "Latest release"
  fi
  if [[ -d "$WORK/main" ]]; then
    li main main | section "Development"
  fi
  slots "$RELEASE" -rV | lis | section "Releases"
  slots "$PREVIEW" -rV | lis | section "Preview releases"
  slots "$PR" -t- -k2 -rn | lis | section "Pull request previews"
  slots '.' | { grep -Ev "$KNOWN" || true; } | lis | section "Other"
  cat <<EOF
</main>
</body>
</html>
EOF
}

if [[ "$ACTION" == publish ]]; then
  MESSAGE="Publish $(basename -a "$TARGET"/*/ | tr '\n' ' ')from ${GITHUB_REF_NAME:-?} (${GITHUB_SHA:-?})"
else
  MESSAGE="Remove $TARGET (${GITHUB_REF_NAME:-?})"
fi

for attempt in 1 2 3 4 5; do
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

echo "Could not push to $BRANCH after 5 attempts." >&2
exit 1
