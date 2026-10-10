#!/usr/bin/env bash
# 在本机运行（手机已通过 USB 连接并开启「USB 调试」）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"

if [[ ! -f "$APK" ]]; then
  echo "未找到 APK，正在构建…"
  if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
    if [[ -d "$HOME/Library/Android/sdk" ]]; then
      export ANDROID_HOME="$HOME/Library/Android/sdk"
    elif [[ -d "$HOME/Android/Sdk" ]]; then
      export ANDROID_HOME="$HOME/Android/Sdk"
    fi
  fi
  if [[ -n "${ANDROID_HOME:-}" ]]; then
    echo "sdk.dir=$ANDROID_HOME" > "$ROOT/local.properties"
  fi
  cd "$ROOT"
  ./gradlew assembleDebug
fi

if ! command -v adb >/dev/null 2>&1; then
  echo "未找到 adb。请安装 Android platform-tools，或把 SDK 的 platform-tools 加入 PATH。"
  exit 1
fi

echo "已连接设备："
adb devices -l
DEVICE_COUNT=$(adb devices | awk 'NR>1 && $2=="device"{c++} END{print c+0}')
if [[ "$DEVICE_COUNT" -lt 1 ]]; then
  echo ""
  echo "未检测到已授权的设备。请确认："
  echo "  1. 手机已开启「开发者选项」→「USB 调试」"
  echo "  2. 弹窗里点了「允许这台计算机调试」"
  echo "  3. USB 线为数据线（非仅充电）"
  exit 1
fi

adb install -r "$APK"
echo ""
echo "安装完成。在手机上打开「热帖」即可。"
