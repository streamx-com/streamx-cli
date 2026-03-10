#!/usr/bin/env sh
# install.sh - Install StreamX CLI
#
# Usage:
#   curl -fsSL https://raw.githubusercontent.com/streamx-com/streamx-cli/main/install.sh | sh
#   curl -fsSL https://raw.githubusercontent.com/streamx-com/streamx-cli/main/install.sh | sh -s -- --version 1.0.0
#   curl -fsSL https://raw.githubusercontent.com/streamx-com/streamx-cli/main/install.sh | sh -s -- --version 1.0.0-rc.1.abc1234 --dest /usr/local/bin

set -eu

REPO_STABLE="streamx-com/streamx-cli"
REPO_PREVIEW="streamx-com/streamx-cli-preview"
BINARY_NAME="streamx"
INSTALL_DIR="."
VERSION=""

# ---------------------------------------------------------------------------
# Parse arguments
# ---------------------------------------------------------------------------
while [ $# -gt 0 ]; do
  case "$1" in
    --version|-v) VERSION="$2"; shift 2 ;;
    --dest|-d)    INSTALL_DIR="$2"; shift 2 ;;
    --help|-h)
      echo "Usage: install.sh [--version <version>] [--dest <directory>]"
      echo ""
      echo "Options:"
      echo "  --version, -v   Version to install (default: latest stable release)"
      echo "  --dest, -d      Installation directory (default: current directory)"
      exit 0
      ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
info()  { echo "[streamx] $*"; }
error() { echo "[streamx] ERROR: $*" >&2; exit 1; }

need_cmd() {
  if ! command -v "$1" > /dev/null 2>&1; then
    error "Required command '$1' not found. Please install it and retry."
  fi
}

# Compute SHA-256 using whichever tool is available.
# sha256sum is standard on Linux; shasum ships with macOS.
compute_sha256() {
  if command -v sha256sum > /dev/null 2>&1; then
    sha256sum "$1" | cut -d ' ' -f 1
  elif command -v shasum > /dev/null 2>&1; then
    shasum -a 256 "$1" | cut -d ' ' -f 1
  else
    return 1
  fi
}

# ---------------------------------------------------------------------------
# Detect platform
# ---------------------------------------------------------------------------
detect_platform() {
  OS=$(uname -s | tr '[:upper:]' '[:lower:]')
  ARCH=$(uname -m)

  case "$OS" in
    linux*)  OS="linux" ;;
    darwin*) OS="macos" ;;
    *)       error "Unsupported operating system: $OS" ;;
  esac

  case "$ARCH" in
    x86_64|amd64)  ARCH="x86_64" ;;
    aarch64|arm64) ARCH="aarch64" ;;
    *)             error "Unsupported architecture: $ARCH" ;;
  esac

  ARTIFACT="streamx-${OS}-${ARCH}"
}

# ---------------------------------------------------------------------------
# Resolve version & repo
# ---------------------------------------------------------------------------
resolve_version() {
  if [ -n "$VERSION" ]; then
    # Preview versions contain "-rc."
    case "$VERSION" in
      *-rc.*) REPO="$REPO_PREVIEW" ;;
      *)      REPO="$REPO_STABLE" ;;
    esac
    TAG="$VERSION"
  else
    REPO="$REPO_STABLE"
    TAG="latest"
  fi
}

# ---------------------------------------------------------------------------
# Download & install
# ---------------------------------------------------------------------------
download_and_install() {
  need_cmd uname
  need_cmd chmod
  need_cmd mkdir

  TMPDIR=$(mktemp -d)
  trap 'rm -rf "$TMPDIR"' EXIT

  if [ "$TAG" = "latest" ]; then
    DOWNLOAD_URL="https://github.com/${REPO}/releases/latest/download/${ARTIFACT}"
  else
    DOWNLOAD_URL="https://github.com/${REPO}/releases/download/${TAG}/${ARTIFACT}"
  fi

  info "Downloading ${BINARY_NAME} from ${DOWNLOAD_URL} ..."

  if command -v curl > /dev/null 2>&1; then
    HTTP_CODE=$(curl -#fSL -w "%{http_code}" -o "${TMPDIR}/${ARTIFACT}" "$DOWNLOAD_URL") || true
    if [ "$HTTP_CODE" != "200" ]; then
      error "Download failed (HTTP ${HTTP_CODE}). Check that version '${TAG}' exists at https://github.com/${REPO}/releases"
    fi
  elif command -v wget > /dev/null 2>&1; then
    wget --show-progress -q -O "${TMPDIR}/${ARTIFACT}" "$DOWNLOAD_URL" || error "Download failed. Check that version '${TAG}' exists at https://github.com/${REPO}/releases"
  else
    error "Neither 'curl' nor 'wget' found. Please install one of them and retry."
  fi

  # ---------------------------------------------------------------------------
  # Verify checksum
  # ---------------------------------------------------------------------------
  CHECKSUMS_URL="${DOWNLOAD_URL%/*}/checksums_sha256.txt"
  info "Verifying checksum ..."

  CHECKSUMS_FILE="${TMPDIR}/checksums_sha256.txt"
  if command -v curl > /dev/null 2>&1; then
    curl -fsSL -o "$CHECKSUMS_FILE" "$CHECKSUMS_URL" 2>/dev/null || true
  elif command -v wget > /dev/null 2>&1; then
    wget -q -O "$CHECKSUMS_FILE" "$CHECKSUMS_URL" 2>/dev/null || true
  fi

  if [ -s "$CHECKSUMS_FILE" ]; then
    EXPECTED=$(grep "${ARTIFACT}$" "$CHECKSUMS_FILE" | cut -d ' ' -f 1)
    if [ -n "$EXPECTED" ]; then
      ACTUAL=$(compute_sha256 "${TMPDIR}/${ARTIFACT}") || error "No SHA-256 tool found (sha256sum or shasum). Cannot verify checksum."
      if [ "$EXPECTED" = "$ACTUAL" ]; then
        info "Checksum verified: ${ACTUAL}"
      else
        error "Checksum mismatch! Expected ${EXPECTED}, got ${ACTUAL}. The download may be corrupted."
      fi
    else
      info "Warning: no checksum entry found for ${ARTIFACT}, skipping verification."
    fi
  else
    info "Warning: checksums file not available, skipping verification."
  fi

  chmod +x "${TMPDIR}/${ARTIFACT}"

  # Install to destination
  mkdir -p "$INSTALL_DIR" 2>/dev/null || true
  if [ -w "$INSTALL_DIR" ]; then
    mv "${TMPDIR}/${ARTIFACT}" "${INSTALL_DIR}/${BINARY_NAME}"
  else
    info "Elevated permissions required to install to ${INSTALL_DIR}"
    sudo mv "${TMPDIR}/${ARTIFACT}" "${INSTALL_DIR}/${BINARY_NAME}"
  fi

  # Resolve to absolute path for clear messaging
  INSTALL_DIR=$(cd "$INSTALL_DIR" && pwd)

  info "Installed ${BINARY_NAME} to ${INSTALL_DIR}/${BINARY_NAME}"
}

# ---------------------------------------------------------------------------
# Verify installation
# ---------------------------------------------------------------------------
verify() {
  # Determine how the user should invoke the binary
  case ":$PATH:" in
    *:"$INSTALL_DIR":*) STREAMX_CMD="streamx" ;;
    *)                  STREAMX_CMD="${INSTALL_DIR}/streamx" ;;
  esac

  if [ -x "${INSTALL_DIR}/${BINARY_NAME}" ]; then
    INSTALLED_VERSION=$("${INSTALL_DIR}/${BINARY_NAME}" --version 2>/dev/null || echo "unknown")
    info "Verified: ${INSTALLED_VERSION}"
  else
    error "Installation could not be verified."
  fi

  if ! echo ":$PATH:" | grep -q ":${INSTALL_DIR}:"; then
    info "Note: ${INSTALL_DIR} is not in your PATH. Add it with:"
    echo ""
    echo "  export PATH=\"${INSTALL_DIR}:\$PATH\""
    echo ""
  fi
}

# ---------------------------------------------------------------------------
# Print usage
# ---------------------------------------------------------------------------
print_usage() {
  echo ""
  echo "StreamX CLI installed successfully!"
  echo ""
  echo "Get started:"
  echo ""
  echo "  ${STREAMX_CMD} --help          Show all available commands"
  echo "  ${STREAMX_CMD} --version       Print installed version"
  echo ""
}

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
detect_platform
resolve_version
download_and_install
verify
print_usage