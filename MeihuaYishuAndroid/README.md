# 梅花易数 — Android

Kotlin + Jetpack Compose 本地起卦 / 解盘 App。

- **时间起卦**（先天数：年支 + 月 + 日 + 时支）
- **数字起卦** / **手动选卦** / **随机起卦**
- 本卦 · 互卦 · 变卦 · 体用生克规则纲要
- **导出解盘图片 + AI 提示词**，可分享给 ChatGPT / Claude 等解读
- 可选应用内 AI（设置中填写 OpenAI 兼容 Base URL / Model / API Key）
- 历史：Room 本地存储
- **无自建后端**；不请求定位权限

## 自行构建

要求：JDK 17+、Android SDK 34。

```bash
cd MeihuaYishuAndroid
# local.properties 中配置 sdk.dir=
./gradlew assembleDebug
# 输出：app/build/outputs/apk/debug/app-debug.apk
```

单元测试：

```bash
./gradlew :app:testDebugUnitTest
```

## 模块

| 路径 | 作用 |
|------|------|
| `app/.../engine/` | 起卦、本互变、体用、文王卦名、规则纲要 |
| `app/.../export/` | 解盘位图渲染与分享 |
| `app/.../ui/` | 起卦 / 解盘 / 解读 / 历史 / 设置 |
| `app/.../ai/` | 可选 OpenAI 兼容客户端 |
| `app/.../data/` | Room 历史 |

## 隐私

- 默认本地起卦与历史
- 导出图片时，解盘内容经系统分享面板发往你选择的应用
- 启用应用内 AI 时，所问与盘面发往你配置的第三方 API；Key 存 EncryptedSharedPreferences
