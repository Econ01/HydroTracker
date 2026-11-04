#!/usr/bin/env bash
#
# HydroTracker local update-test APK builder.
#
# Builds controlled GitHub debug APKs for the local updater harness. The normal
# githubDebug signing configuration is preserved; only the deliberately invalid
# bad-signature copy is re-signed with a generated test-only key.
#
# Usage:
#   ./scripts/prepare_update_test_apks.sh
#
# Outputs are written to build/update-test/.

set -euo pipefail

# Always operate from the repository root, regardless of the caller's cwd.
cd "$(dirname "$0")/.."

info() { printf '%s\n' "$*"; }
fail() { printf 'error: %s\n' "$*" >&2; exit 1; }

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
    grep '^#' "$0" | sed 's/^# \{0,1\}//'
    exit 0
elif [[ "$#" -ne 0 ]]; then
    fail "Unknown argument: $1 (try --help)"
fi

OUTPUT_DIR="build/update-test"
APK_SOURCE="app/build/outputs/apk/play/debug/app-play-debug.apk"
APP_BUILD_FILE="app/build.gradle.kts"

# Derive the installed baseline from the current project, then create a synthetic update that is
# newer without assuming what the next public release will be called.
INSTALLED_VERSION_NAME=$(grep -E '^val configuredVersionName = .* \?: "[^"]+"' "$APP_BUILD_FILE" \
    | head -n 1 | sed -E 's/.*\?: "([^"]+)".*/\1/')
INSTALLED_VERSION_CODE=$(grep -E '^val configuredVersionCode = .* \?: [0-9]+' "$APP_BUILD_FILE" \
    | head -n 1 | sed -E 's/.*\?: ([0-9]+).*/\1/')
[[ -n "$INSTALLED_VERSION_NAME" ]] || fail "Could not read configuredVersionName"
[[ "$INSTALLED_VERSION_NAME" =~ ^[0-9]+(\.[0-9]+)*$ ]] \
    || fail "Update testing requires dotted numeric version names, found: $INSTALLED_VERSION_NAME"
[[ "$INSTALLED_VERSION_CODE" =~ ^[0-9]+$ ]] || fail "Could not read configuredVersionCode"
UPDATE_VERSION_NAME="$INSTALLED_VERSION_NAME.1"
UPDATE_VERSION_CODE=$((INSTALLED_VERSION_CODE + 1))

NORMAL_DEBUG_SUFFIX=".debug"
WRONG_PACKAGE_SUFFIX=".wrong"

VALID_APK="$OUTPUT_DIR/HydroTracker-$UPDATE_VERSION_NAME.apk"
OLD_APK="$OUTPUT_DIR/HydroTracker-$INSTALLED_VERSION_NAME.apk"
WRONG_PACKAGE_APK="$OUTPUT_DIR/HydroTracker-$UPDATE_VERSION_NAME-wrong-package.apk"
BAD_SIGNATURE_APK="$OUTPUT_DIR/HydroTracker-$UPDATE_VERSION_NAME-bad-signature.apk"
TEST_KEYSTORE="$OUTPUT_DIR/bad-signature-test.keystore"
RESEIGN_INPUT="$OUTPUT_DIR/.bad-signature-source.apk"

mkdir -p "$OUTPUT_DIR"

build_apk() {
    local version_code="$1"
    local version_name="$2"
    local application_id_suffix="$3"
    local destination="$4"
    local description="$5"

    info "Building $description (code $version_code, name $version_name, suffix $application_id_suffix)…"
    # Removing the previous copy ensures that a failed or misconfigured build cannot be
    # mistaken for a newly generated test artefact.
    rm -f "$APK_SOURCE"
    ./gradlew assemblePlayDebug --console=plain \
        "-PhydroVersionCode=$version_code" \
        "-PhydroVersionName=$version_name" \
        "-PhydroApplicationIdSuffix=$application_id_suffix"

    [[ -s "$APK_SOURCE" ]] || fail "Expected GitHub debug APK missing at $APK_SOURCE"
    cp "$APK_SOURCE" "$destination"
}

build_apk \
    "$UPDATE_VERSION_CODE" \
    "$UPDATE_VERSION_NAME" \
    "$NORMAL_DEBUG_SUFFIX" \
    "$VALID_APK" \
    "valid update"

build_apk \
    "$INSTALLED_VERSION_CODE" \
    "$INSTALLED_VERSION_NAME" \
    "$NORMAL_DEBUG_SUFFIX" \
    "$OLD_APK" \
    "installed baseline and old-version update"

build_apk \
    "$UPDATE_VERSION_CODE" \
    "$UPDATE_VERSION_NAME" \
    "$WRONG_PACKAGE_SUFFIX" \
    "$WRONG_PACKAGE_APK" \
    "wrong-package update"

# Locate apksigner in PATH or the numerically newest Android build-tools directory,
# matching the release script's lookup behaviour.
APKSIGNER="$(command -v apksigner || true)"
if [[ -z "$APKSIGNER" ]]; then
    SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
    BUILD_TOOLS_DIR=$(ls -d "$SDK_DIR"/build-tools/*/ 2>/dev/null \
        | awk -F/ '{print $(NF-1), $0}' \
        | sort -t. -k1,1n -k2,2n -k3,3n \
        | tail -n 1 | cut -d' ' -f2- || true)
    if [[ -n "$BUILD_TOOLS_DIR" && -x "${BUILD_TOOLS_DIR}apksigner" ]]; then
        APKSIGNER="${BUILD_TOOLS_DIR}apksigner"
    fi
fi
[[ -n "$APKSIGNER" ]] || fail "apksigner not found; install Android SDK build-tools or add it to PATH"
command -v keytool >/dev/null 2>&1 || fail "keytool not found; run this script with a JDK on PATH"

# This fixed, non-secret password protects only a disposable local test key. Never use
# this keystore for a distribution build.
TEST_KEY_ALIAS="hydrotracker-update-test"
TEST_KEY_PASSWORD="hydro-update-test"
rm -f "$TEST_KEYSTORE" "$BAD_SIGNATURE_APK" "$RESEIGN_INPUT"
trap 'rm -f "$RESEIGN_INPUT"' EXIT

info "Generating a disposable bad-signature key and re-signing a copy…"
keytool -genkeypair -noprompt \
    -keystore "$TEST_KEYSTORE" \
    -storepass "$TEST_KEY_PASSWORD" \
    -keypass "$TEST_KEY_PASSWORD" \
    -alias "$TEST_KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 3650 \
    -dname "CN=HydroTracker Update Test, OU=Local Testing, O=HydroTracker"

cp "$VALID_APK" "$RESEIGN_INPUT"
"$APKSIGNER" sign \
    --ks "$TEST_KEYSTORE" \
    --ks-key-alias "$TEST_KEY_ALIAS" \
    --ks-pass "pass:$TEST_KEY_PASSWORD" \
    --key-pass "pass:$TEST_KEY_PASSWORD" \
    --out "$BAD_SIGNATURE_APK" \
    "$RESEIGN_INPUT"

rm -f "$RESEIGN_INPUT"
trap - EXIT
[[ -s "$BAD_SIGNATURE_APK" ]] || fail "Bad-signature APK was not created"

# Check that every artefact has a structurally valid signature. The bad-signature APK
# must verify against its own test key; it is invalid only for the installed app.
for apk in "$VALID_APK" "$OLD_APK" "$WRONG_PACKAGE_APK" "$BAD_SIGNATURE_APK"; do
    "$APKSIGNER" verify "$apk" >/dev/null || fail "APK signature verification failed: $apk"
done

# The server consumes this manifest so no release names or codes are hardcoded in either script.
export OUTPUT_DIR INSTALLED_VERSION_NAME INSTALLED_VERSION_CODE UPDATE_VERSION_NAME UPDATE_VERSION_CODE
export VALID_APK OLD_APK WRONG_PACKAGE_APK BAD_SIGNATURE_APK
python3 - <<'PY'
import json
import os
from pathlib import Path

root = Path(os.environ["OUTPUT_DIR"])
config = {
    "baselineVersionName": os.environ["INSTALLED_VERSION_NAME"],
    "baselineVersionCode": int(os.environ["INSTALLED_VERSION_CODE"]),
    "updateVersionName": os.environ["UPDATE_VERSION_NAME"],
    "updateVersionCode": int(os.environ["UPDATE_VERSION_CODE"]),
    "artefacts": {
        "valid": os.path.basename(os.environ["VALID_APK"]),
        "old": os.path.basename(os.environ["OLD_APK"]),
        "wrong_package": os.path.basename(os.environ["WRONG_PACKAGE_APK"]),
        "bad_signature": os.path.basename(os.environ["BAD_SIGNATURE_APK"]),
    },
}
(root / "test-config.json").write_text(json.dumps(config, indent=2) + "\n", encoding="utf-8")
PY

info ""
info "Update-test artefacts prepared under $OUTPUT_DIR:"
info "  Valid update:       $VALID_APK"
info "  Installed/old APK:  $OLD_APK"
info "  Wrong package:      $WRONG_PACKAGE_APK"
info "  Bad signature:      $BAD_SIGNATURE_APK"
info "  Test-only keystore: $TEST_KEYSTORE"
info ""
info "The valid, old and wrong-package APKs retain the normal local playDebug signing."
info "Nothing has been uploaded or published."
