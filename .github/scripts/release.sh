#!/usr/bin/env bash
# release.sh - Usage: ./release.sh [patch|minor|major|preview] [-y]

set -euo pipefail

# Parse arguments
AUTO_CONFIRM=false
TYPE=patch
for arg in "$@"; do
  case "$arg" in
    -y) AUTO_CONFIRM=true ;;
    *) TYPE="$arg" ;;
  esac
done

BRANCH=$(git rev-parse --abbrev-ref HEAD)

# Get current version from pom.xml
CURRENT=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "Current version: $CURRENT"
echo "Branch: $BRANCH"

# Ensure it's a SNAPSHOT
if [[ "$CURRENT" != *-SNAPSHOT ]]; then
  echo "Error: Current version ($CURRENT) is not a SNAPSHOT. Nothing to release."
  exit 1
fi

# Strip -SNAPSHOT
BASE=${CURRENT%-SNAPSHOT}

confirm() {
  local message="$1"
  if [[ "$AUTO_CONFIRM" == true ]]; then
    echo "$message Auto-confirmed with -y flag."
    return 0
  fi
  read -p "$message [y/N] " -n 1 -r
  echo
  [[ $REPLY =~ ^[Yy]$ ]] || { echo "Aborted."; exit 0; }
}

if [[ "$TYPE" == "preview" ]]; then
  # Find the next rc number by checking existing tags
  LAST_RC=$(git tag -l "${BASE}-rc.*" | sed "s/${BASE}-rc\.//" | sed 's/\..*//' | sort -n | tail -1)
  NEXT_RC=$(( ${LAST_RC:-0} + 1 ))

  SHORT_SHA=$(git rev-parse --short HEAD)
  PREVIEW="${BASE}-rc.${NEXT_RC}.${SHORT_SHA}"

  echo ""
  echo "Preview version: $PREVIEW"
  echo ""
  confirm "Are you sure you want to publish preview $PREVIEW?"

  # Set rc version, commit, tag
  mvn versions:set -DnewVersion="$PREVIEW" -DgenerateBackupPoms=false
  git add pom.xml
  git commit -m "Preview $PREVIEW"
  git tag "$PREVIEW"

  # Restore SNAPSHOT version, commit
  mvn versions:set -DnewVersion="$CURRENT" -DgenerateBackupPoms=false
  git add pom.xml
  git commit -m "Prepare for next development iteration $CURRENT [skip ci]"

  # Push branch first, then tag
  git push origin "$BRANCH"
  git push origin "$PREVIEW"

  echo "Done! Preview $PREVIEW started. Follow the CI job progress."
  exit 0
fi

IFS='.' read -r MAJOR MINOR PATCH <<< "$BASE"

# Calculate release version
RELEASE="${MAJOR}.${MINOR}.${PATCH}"

# Calculate next SNAPSHOT version
case "$TYPE" in
  patch) NEXT="${MAJOR}.${MINOR}.$((PATCH + 1))-SNAPSHOT" ;;
  minor) NEXT="${MAJOR}.$((MINOR + 1)).0-SNAPSHOT" ;;
  major) NEXT="$((MAJOR + 1)).0.0-SNAPSHOT" ;;
  *) echo "Usage: $0 [patch|minor|major|preview] [-y]"; exit 1 ;;
esac

echo ""
echo "Release version:         $RELEASE"
echo "Next development version: $NEXT"
echo ""
confirm "Are you sure you want to release $RELEASE?"

# Set release version, commit, tag
mvn versions:set -DnewVersion="$RELEASE" -DgenerateBackupPoms=false
git add pom.xml
git commit -m "Release $RELEASE"
git tag "$RELEASE"

# Set next SNAPSHOT version, commit
mvn versions:set -DnewVersion="$NEXT" -DgenerateBackupPoms=false
git add pom.xml
git commit -m "Prepare for next development iteration $NEXT [skip ci]"

# Push branch first (skipped by CI), then tag (triggers the real build+release)
git push origin "$BRANCH"
git push origin "$RELEASE"

echo "Done! Release $RELEASE started. Follow the CI job progress. Updated to $NEXT"