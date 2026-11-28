# 紫微斗数 — Android

Kotlin + Jetpack Compose 本地排盘 App，三合派安星 MVP。

- 公历 / 农历生辰（1900–2100）、性别、姓名备注
- 命宫·身宫·十二宫、五行局、紫微/天府星系十四主星
- 左辅右弼、昌曲、魁钺、禄存、羊陀、火铃、空劫、天马
- **生年四化**（禄权科忌）落星标注
- **本命 / 大限 / 流年 / 流月**切换：对应干支四化与宫名重布
- **飞星 / 自化**：宫干飞化一览，命宫相关与自化高亮
- 大限起运、宫位点选详情、规则解读
- 可选 AI：设置中填写 OpenAI 兼容 Base URL / Model / API Key
- 历史：Room 本地存储
- **无自建后端**；不请求定位权限

## 下载安装（GitHub）

预构建 debug APK（v1.2.0，可侧载）：

- 文件：[`dist/ZiWeiDouShu.apk`](https://github.com/coffeeli328/BPB-Worker-Panel/blob/cursor/ziwei-doushu-android-7ccb/ZiWeiDouShuAndroid/dist/ZiWeiDouShu.apk)
- 直链：https://github.com/coffeeli328/BPB-Worker-Panel/raw/cursor/ziwei-doushu-android-7ccb/ZiWeiDouShuAndroid/dist/ZiWeiDouShu.apk
- 源码目录：https://github.com/coffeeli328/BPB-Worker-Panel/tree/cursor/ziwei-doushu-android-7ccb/ZiWeiDouShuAndroid
- Pull Request：https://github.com/coffeeli328/BPB-Worker-Panel/pull/15

### 安装步骤

1. 用手机浏览器打开上方直链下载 APK
2. 系统设置 → 允许「安装未知应用」
3. 打开 APK → 安装 → 「紫微斗数」

合并进 `main` 后，可将链接中的分支名换成 `main`。

## 自行构建

要求：JDK 17+、Android SDK 34。

```bash
cd ZiWeiDouShuAndroid
# local.properties 中 sdk.dir=你的 Android SDK 路径
./gradlew assembleDebug
# 输出：app/build/outputs/apk/debug/app-debug.apk
# 亦可复制为 dist/ZiWeiDouShu.apk
```

单元测试：

```bash
./gradlew :app:testDebugUnitTest
```

## 模块

| 路径 | 作用 |
|------|------|
| `app/.../engine/` | 农历、安星、庙旺、规则解读 |
| `app/.../ui/` | 排盘 / 命盘 / 解读 / 历史 / 设置 |
| `app/.../ai/` | 可选 OpenAI 兼容客户端 |
| `app/.../data/` | Room 历史 |

## 说明

- 安星与四化口径为常见三合派口诀，供学习参考；派别口诀或有差异
- 流年命宫取流年地支宫；流月以流年支起正月顺数；飞星为本命十二宫干飞化
- Release 未配置正式签名（侧载用 debug 签名）

## 隐私

- 默认本地排盘与历史
- 启用 AI 时，命盘结构发往你配置的第三方 API；Key 存 EncryptedSharedPreferences
