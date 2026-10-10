# 热帖 · Android

Kotlin + Jetpack Compose 版「热帖」，与 `x-hotpost/` Web 控制台能力对齐：

- 本机抓热点（Google News RSS + 演示样本）
- 自动生成待审草稿
- **人工审核**后发布
- **不用 X API**：关闭演示模式后，跳转到 X/Twitter 应用发帖（文案已填好，你在 X 里点发送）
- WorkManager 后台定时「跑一轮」（抓热点 + 写草稿；演示模式可自动记为已发布）

## 下载 APK（推荐 Releases）

GitHub **Releases** 比分支里的 raw 文件更稳（约 17MB）：

https://github.com/coffeeli328/BPB-Worker-Panel/releases/download/x-hotpost-v1.0.2/XHotpost.apk

发布页：https://github.com/coffeeli328/BPB-Worker-Panel/releases/tag/x-hotpost-v1.0.2

若浏览器一直卡在 0%：换 Chrome/Safari、换网络，或用电脑下载后传到手机。

## 安装到已连接的手机（本机 USB）

云 Agent **无法**访问你电脑上的 USB，需要在你自己的电脑上执行（手机已连上并开启 USB 调试）：

```bash
cd XHotpostAndroid
chmod +x scripts/install-to-phone.sh
./scripts/install-to-phone.sh
```

脚本会自动 `assembleDebug`（若还没有 APK）并用 `adb install -r` 安装。

## 构建 APK

要求：JDK 17+、Android SDK 34。

```bash
cd XHotpostAndroid
# local.properties 示例：
# sdk.dir=/path/to/Android/sdk
./gradlew assembleDebug
# 输出 app/build/outputs/apk/debug/app-debug.apk
```

单元测试：

```bash
./gradlew :app:testDebugUnitTest
```

## 使用

1. 安装 debug APK（侧载，与奇门 Android 相同流程）
2. 打开 App → 默认**演示模式**，可先体验审核流
3. 设置里选赛道、填变现链接，关闭演示模式
4. 审核通过 → 自动打开 X 应用（需已安装 X/Twitter）
5. 在 X 里确认发送即可

## 与 Web 版的差异

| 能力 | Web `x-hotpost` | Android |
|------|-----------------|---------|
| 热点 / 草稿 | ✅ | ✅ |
| 后台定时 | Node 定时器 | WorkManager |
| 真发帖 | 本机 Chromium 会话 | 跳转 X 应用 / 网页 intent |
| X API | ❌ | ❌ |

## 隐私

- 默认仅请求 **INTERNET**（拉 RSS）
- 草稿与设置存本机 Room / DataStore
- 不向第三方上传账号密码
