# 奇门遁甲 — Android

Kotlin + Jetpack Compose 本地排盘 App，与 iOS `QimenDunjia/` 功能对齐的 MVP。

- **时家奇门 · 转盘 · 拆补（默认）· 可选置闰**
- 真太阳时 / 节气（Meeus 近似，与 iOS 同源口径）
- 九宫盘面、所问之事、用神规则解读
- 可选 AI：设置中填写 OpenAI 兼容 Base URL / Model / API Key
- 历史：Room 本地存储
- **无自建后端**；不请求定位权限

## 直接安装 APK（侧载）

预构建 APK（debug 签名，可安装）：

- Agent Store：`/cursor/stores/bc-1bfe3aab-fbad-44f1-8b19-b0a2acba2c2d/media/QimenDunjia.apk`
- 本目录：`dist/QimenDunjia.apk`（构建后生成）

### 安装步骤

1. 把 `QimenDunjia.apk` 传到手机（文件管理 / 网盘 / USB）
2. 系统设置 → 安全 → 允许「安装未知应用」/ 对应文件管理器的安装权限
3. 打开 APK → 安装 → 打开「奇门遁甲」
4. 若提示「未知来源」被拦截，在弹窗中允许该来源后重试

不需要 Google Play。卸载后重新安装即可升级。

## 自行构建

要求：JDK 17+、Android SDK 34、本机已装 cmdline-tools。

```bash
cd QimenDunjiaAndroid
# 编辑 local.properties 指向你的 SDK，例如：
# sdk.dir=/path/to/Android/sdk
./gradlew assembleDebug
# 输出：app/build/outputs/apk/debug/app-debug.apk
```

单元测试（排盘黄金用例）：

```bash
./gradlew :app:testDebugUnitTest
```

## 模块

| 路径 | 作用 |
|------|------|
| `app/.../engine/` | 定局、排盘、天文、真太阳时、置闰、用神解读 |
| `app/.../ui/` | 起局 / 盘面 / 解读 / 历史 / 设置 |
| `app/.../ai/` | 可选 OpenAI 兼容客户端 |
| `app/.../data/` | Room 历史 |

## 与 iOS 的差异（已知）

- UI 为 Material3，非 SwiftUI 像素对齐
- 农历展示为简化/系统日历口径，未做完整农历换算 UI
- Release 未配置正式签名密钥（侧载用 debug 签名 APK）
- App 图标为简易九宫占位

## 隐私

- 默认本地排盘与历史
- 启用 AI 时，所问与盘面发往你配置的第三方 API；Key 存 EncryptedSharedPreferences
