# 话题雷达 (Topic Radar) — iOS

SwiftUI 本地话题聚合 App：把公开 **RSS** 源里关于某个话题的资讯，收成可浏览的情报流。

预置示例：**温哥华房价**（Google News 中/英、CBC BC、Reddit `r/vancouverhousing`）。

- 采集与存储均在 **本机**（SwiftData），无需自建后端
- 同仓库还有 Web 版参考实现：`topic-radar/`

## 要求

- macOS + **Xcode 15+**
- iOS 17+ 模拟器或真机
- 上架 / TestFlight：付费 **Apple Developer Program**

## 打开与运行

```bash
git clone https://github.com/coffeeli328/BPB-Worker-Panel.git
cd BPB-Worker-Panel
# 打开本目录工程
open TopicRadar/TopicRadar.xcodeproj
```

1. Target **TopicRadar** → **Signing & Capabilities**
   - 选择你的 **Team**（仓库内为空）
   - 将 Bundle ID 从占位 `com.gglee.TopicRadar` 改成**你的唯一 ID**
2. 模拟器 iOS 17+ → **Product → Run**（⌘R）
3. 自测：首页点「立即采集全部」→ 进入「温哥华房价」→ 搜索 / 管理源

显示名默认为 **话题雷达**。

## 功能

| 能力 | 说明 |
|------|------|
| 话题 | 新建话题、关键词过滤 |
| 信息源 | 添加 / 启用 / 停用 / 删除 RSS |
| 采集 | URLSession 拉取 + 系统 XMLParser 解析 |
| 阅读 | 列表、摘要、跳转原文、本地搜索 |
| 示例 | 首次启动自动写入「温哥华房价」 |

## 建议的信息源

1. Google News RSS：`https://news.google.com/rss/search?q=关键词&hl=zh-CN&gl=CA&ceid=CA:zh-Hans`
2. 媒体站点公开 `/rss` 或 `/feed`
3. 社区 `.rss`（如 Reddit）

不建议一上来做任意网页全文爬虫。

## 测试（⌘U）

**Product → Test**，覆盖关键词匹配与话题源过滤逻辑。

## Archive / TestFlight

详见 [`SIGNING.md`](SIGNING.md)。
