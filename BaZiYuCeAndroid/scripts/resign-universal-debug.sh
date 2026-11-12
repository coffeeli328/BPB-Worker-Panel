#!/usr/bin/env bash
# Ensure sideload-friendly v1+v2 signatures on the debug APK.
# AGP 8 may omit usable v1 when minSdk>=24; some OEM installers still want JAR (v1) blocks.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK_IN="${1:-$ROOT/app/build/outputs/apk/debug/app-debug.apk}"
APK_OUT="${2:-$APK_IN}"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
APKSIGNER="$SDK/build-tools/34.0.0/apksigner"
ZIPALIGN="$SDK/build-tools/34.0.0/zipalign"
DEBUG_KS="${DEBUG_KEYSTORE:-$HOME/.android/debug.keystore}"

if [[ ! -f "$APK_IN" ]]; then
  echo "Missing APK: $APK_IN" >&2
  exit 1
fi
if [[ ! -x "$APKSIGNER" ]]; then
  echo "apksigner not found under $SDK" >&2
  exit 1
fi
if [[ ! -f "$DEBUG_KS" ]]; then
  keytool -genkeypair -keystore "$DEBUG_KS" -storepass android -alias androiddebugkey \
    -keypass android -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US"
fi

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
cp "$APK_IN" "$WORK/in.apk"
(
  cd "$WORK"
  zip -q -d in.apk 'META-INF/*.SF' 'META-INF/*.RSA' 'META-INF/*.DSA' 'META-INF/MANIFEST.MF' 2>/dev/null || true
  "$ZIPALIGN" -f -p 4 in.apk aligned.apk
  # min-sdk-version 1 forces JAR v1 blocks even when APK manifest minSdk is 24+
  "$APKSIGNER" sign \
    --ks "$DEBUG_KS" --ks-pass pass:android --ks-key-alias androiddebugkey --key-pass pass:android \
    --v1-signing-enabled true \
    --v2-signing-enabled true \
    --v3-signing-enabled false \
    --v4-signing-enabled false \
    --min-sdk-version 1 \
    --out signed.apk \
    aligned.apk
)
cp "$WORK/signed.apk" "$APK_OUT"
echo "Signed → $APK_OUT"
"$APKSIGNER" verify --verbose --min-sdk-version 15 "$APK_OUT" | head -8
