#!/usr/bin/env bash
#
# HydroTracker release builder.
#
# Builds the signed Play AAB plus GitHub and F-Droid APKs in one go, renames them
# to "HydroTracker-<versionName>.aab", "HydroTracker-<versionName>.apk" and
# "HydroTracker-<versionName>-fdroid.apk", and collects them under dist/.
#
# Usage:
#   ./scripts/release.sh                 Build a release (pre-flight checks enforced)
#   ./scripts/release.sh --allow-dirty   Skip the clean-working-tree check
#
# The version is read from app/build.gradle.kts (versionName / versionCode),
# which remains the single source of truth for releases.
#
# Output is colour-coded; set NO_COLOR=1 to disable colours.

set -euo pipefail

# Always operate from the repository root, regardless of the caller's cwd.
cd "$(dirname "$0")/.."

# --- Colours (respect the NO_COLOR convention) --------------------------------

if [[ -z "${NO_COLOR:-}" ]]; then
    C_RED='\033[0;31m'
    C_GREEN='\033[0;32m'
    C_YELLOW='\033[0;33m'
    C_BLUE='\033[0;34m'
    C_BOLD='\033[1m'
    C_RESET='\033[0m'
else
    C_RED='' C_GREEN='' C_YELLOW='' C_BLUE='' C_BOLD='' C_RESET=''
fi

info()    { printf '%b%b%b\n' "$C_BLUE"   "$*" "$C_RESET"; }
success() { printf '%b%b%b\n' "$C_GREEN"  "$*" "$C_RESET"; }
warn()    { printf '%b%b%b\n' "$C_YELLOW" "$*" "$C_RESET"; }
fail()    { printf '%b%b%b\n' "$C_RED"    "$*" "$C_RESET" >&2; exit 1; }
header()  { printf '%b%b%b\n' "$C_BOLD"   "$*" "$C_RESET"; }

APP_NAME="HydroTracker"
APP_BUILD_FILE="app/build.gradle.kts"
CHANGELOG_FILE="app/src/main/assets/CHANGELOG.md"
DIST_DIR="dist"
EXPECTED_CERT_SHA256="9455bb56bd4c5352f9d8308e46b94aaa7014ccb8301778556e65bc0a9f943140"
ALLOW_DIRTY=0

for arg in "$@"; do
    case "$arg" in
        --allow-dirty)
            ALLOW_DIRTY=1
            ;;
        -h|--help)
            grep '^#' "$0" | sed 's/^# \{0,1\}//'
            exit 0
            ;;
        *)
            fail "Unknown argument: $arg (try --help)"
            ;;
    esac
done

header "Running pre-flight checks…"

# --- 1. Read the version from the module build script -------------------------

[[ -f "$APP_BUILD_FILE" ]] || fail "$APP_BUILD_FILE not found"

VERSION_NAME=$(grep -E '^val configuredVersionName = .* \?: "[^"]+"' "$APP_BUILD_FILE" \
    | head -n 1 | sed -E 's/.*\?: "([^"]+)".*/\1/')
VERSION_CODE=$(grep -E '^val configuredVersionCode = .* \?: [0-9]+' "$APP_BUILD_FILE" \
    | head -n 1 | sed -E 's/.*\?: ([0-9]+).*/\1/')

[[ -n "$VERSION_NAME" ]] || fail "Could not read versionName from $APP_BUILD_FILE"
[[ -n "$VERSION_CODE" ]] || fail "Could not read versionCode from $APP_BUILD_FILE"

success "   [ok] Version: $VERSION_NAME ($VERSION_CODE)"

# --- 2. Signing: never let a publish build fall back to the debug key ---------

[[ -f signing_keys/upload-keystore ]] \
    || fail "signing_keys/upload-keystore not found — refusing to build a release"

credential_present() {
    local name="$1"
    # Environment variable wins, then local.properties (matching Gradle's behaviour).
    # printenv keeps this portable across bash and zsh (no bash-only ${!name} expansion).
    [[ -n "$(printenv "$name" 2>/dev/null)" ]] && return 0
    [[ -f local.properties ]] && grep -qE "^${name}=.+" local.properties && return 0
    return 1
}

for cred in KEYSTORE_PASSWORD KEY_ALIAS KEY_PASSWORD; do
    credential_present "$cred" \
        || fail "$cred is set neither as an environment variable nor in local.properties"
done

success "   [ok] Signing: upload keystore and credentials present"

# --- 3. The working tree must be clean ----------------------------------------

if [[ "$ALLOW_DIRTY" -eq 1 ]]; then
    warn "   [skip] --allow-dirty: skipping the clean-working-tree check"
elif [[ -n "$(git status --porcelain)" ]]; then
    git status --short
    fail "Working tree is not clean — commit or stash everything before releasing"
else
    success "   [ok] Git: working tree is clean"
fi

# --- 4. The in-app changelog must already document this version ---------------

grep -qE "^\[$VERSION_NAME\]" "$CHANGELOG_FILE" \
    || fail "$CHANGELOG_FILE has no [$VERSION_NAME] entry — write the changelog first"

success "   [ok] Changelog: [$VERSION_NAME] entry found"

# --- 5. Build all three distribution artefacts ----------------------------------

header "Building $APP_NAME $VERSION_NAME (this takes a minute)…"
./gradlew --console=plain bundlePlayRelease assembleGithubRelease assembleFdroidRelease

PLAY_AAB_SRC="app/build/outputs/bundle/playRelease/app-play-release.aab"
GITHUB_APK_SRC="app/build/outputs/apk/github/release/app-github-release.apk"
FDROID_APK_SRC="app/build/outputs/apk/fdroid/release/app-fdroid-release.apk"
[[ -f "$PLAY_AAB_SRC" ]] || fail "Expected Play AAB missing at $PLAY_AAB_SRC"
[[ -f "$GITHUB_APK_SRC" ]] || fail "Expected GitHub APK missing at $GITHUB_APK_SRC"
[[ -f "$FDROID_APK_SRC" ]] || fail "Expected F-Droid APK missing at $FDROID_APK_SRC"

mkdir -p "$DIST_DIR"
PLAY_AAB_OUT="$DIST_DIR/$APP_NAME-$VERSION_NAME.aab"
GITHUB_APK_OUT="$DIST_DIR/$APP_NAME-$VERSION_NAME.apk"
FDROID_APK_OUT="$DIST_DIR/$APP_NAME-$VERSION_NAME-fdroid.apk"
cp "$PLAY_AAB_SRC" "$PLAY_AAB_OUT"
cp "$GITHUB_APK_SRC" "$GITHUB_APK_OUT"
cp "$FDROID_APK_SRC" "$FDROID_APK_OUT"

# --- 6. Verify both APK signatures and their upload-key fingerprints ------------

APKSIGNER="$(command -v apksigner || true)"
if [[ -z "$APKSIGNER" ]]; then
    SDK_DIR="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
    # Pick the numerically highest build-tools directory (plain sort misorders e.g. 36.10 vs 36.9)
    BUILD_TOOLS_DIR=$(ls -d "$SDK_DIR"/build-tools/*/ 2>/dev/null \
        | awk -F/ '{print $(NF-1), $0}' \
        | sort -t. -k1,1n -k2,2n -k3,3n \
        | tail -n 1 | cut -d' ' -f2- || true)
    if [[ -n "$BUILD_TOOLS_DIR" && -x "${BUILD_TOOLS_DIR}apksigner" ]]; then
        APKSIGNER="${BUILD_TOOLS_DIR}apksigner"
    fi
fi

CERT_SHA256=""
verify_apk_certificate() {
    local apk_path="$1"
    local verify_output

    # stderr is discarded: apksigner's Conscrypt dependency emits JDK 24+ native-access
    # warnings there, which are JVM noise rather than a problem with the artefacts.
    if ! verify_output=$("$APKSIGNER" verify --print-certs "$apk_path" 2>/dev/null); then
        fail "APK signature verification failed: $apk_path"
    fi

    CERT_SHA256=$(printf '%s\n' "$verify_output" \
        | sed -nE 's/.*SHA-256 digest: ([0-9a-f]+).*/\1/p' | head -n 1)
    [[ -n "$CERT_SHA256" ]] || fail "Could not read the signer certificate from $apk_path"
    [[ "$CERT_SHA256" == "$EXPECTED_CERT_SHA256" ]] \
        || fail "Unexpected signer certificate in $apk_path: $CERT_SHA256"
}

if [[ -n "$APKSIGNER" ]]; then
    verify_apk_certificate "$GITHUB_APK_OUT"
    GITHUB_CERT_SHA256="$CERT_SHA256"
    verify_apk_certificate "$FDROID_APK_OUT"
    FDROID_CERT_SHA256="$CERT_SHA256"
    [[ "$GITHUB_CERT_SHA256" == "$FDROID_CERT_SHA256" ]] \
        || fail "GitHub and F-Droid APK signer certificates do not match"

    info "Verified the upload-key signature on both APKs:"
    echo "   SHA-256: $CERT_SHA256"
else
    warn "apksigner not found — skipping signature verification for both APKs"
fi

# --- 7. Summary -----------------------------------------------------------------

echo ""
success "Release $VERSION_NAME ($VERSION_CODE) built; nothing has been published:"
ls -lh "$PLAY_AAB_OUT" "$GITHUB_APK_OUT" "$FDROID_APK_OUT" \
    | awk '{print "   " $NF " (" $5 ")"}'
if [[ -n "$CERT_SHA256" ]]; then
    echo ""
    header "For the GitHub release notes (certificate fingerprint — safe to publish):"
    echo "   SHA-256 certificate fingerprint: $CERT_SHA256"
fi
echo ""
header "Manual next steps (not performed by this script):"
echo "   1. Upload $PLAY_AAB_OUT to the Play Console"
echo "   2. Attach all three artefacts in $DIST_DIR/ to GitHub release v$VERSION_NAME"
echo "   3. Tag the release: git tag v$VERSION_NAME && git push origin v$VERSION_NAME"
