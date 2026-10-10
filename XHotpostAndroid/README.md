# 热帖 · Android

Kotlin + Jetpack Compose 版「热帖」，与 `x-hotpost/` Web 控制台能力对齐：

- 本机抓 **X 平台热搜**（公开趋势镜像，不用 X API；演示样本）
- 自动生成待审草稿（多风格模板；可选 OpenAI 兼容 AI 写稿）
- **人工审核**后发布
- **不用 X API**：关闭演示模式后，跳转到 X/Twitter 应用发帖（文案已填好，你在 X 里点发送）
- WorkManager 后台定时「跑一轮」（抓热点 + 写草稿；演示模式可自动记为已发布）

## 提升文案质量

模板发帖容易「像机器」。v1.0.6 起在 **设置 → 内容风格** 可调：

1. **写作风格**：观点 / 干货 / 故事 / 口语 / 专业（同一热点会换不同角度）
2. **人设**：用一两句话描述语气（例如「犀利但真诚，爱用短句」）
3. **可选 AI 写稿**：打开开关，填 OpenAI 兼容 Base URL + Model + API Key（DeepSeek / 通义兼容网关等均可）；失败会自动回退本地模板
4. 改完点 **「按当前风格重写待审草稿」**，或先拒绝旧草稿再「跑一轮」

仍不满意时：在审核台直接改文案再通过——人工微调是变现质量的最后一道闸。

## 下载 APK（推荐 Releases）

GitHub **Releases** 比分支里的 raw 文件更稳（约 17MB）：

https://github.com/coffeeli328/BPB-Worker-Panel/releases/download/x-hotpost-v1.0.7/XHotpost.apk

发布页：https://github.com/coffeeli328/BPB-Worker-Panel/releases/tag/x-hotpost-v1.0.7

### X 热搜说明

设置 →「跟踪 X 热搜」可选地区（美国 / 英国 / 日本 / 新加坡 / 印度，或自动）。
赛道用于**优先排序**匹配词；选「综合 X 热搜」则不过滤。点话题标题会打开 X 实时搜索。

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

- 默认仅请求 **INTERNET**（拉 X 热搜镜像）
- 草稿与设置存本机 Room / DataStore
- 不向第三方上传账号密码
