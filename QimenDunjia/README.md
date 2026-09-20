# 奇门遁甲 (Qimen Dunjia) — iOS

SwiftUI 本地排盘 App：**时家奇门 · 转盘排宫 · 拆补定局（默认）· 可选置闰**。

- **不含**飞盘法；**不请求定位**（城市预设 / 手动经度）
- App Store / Archive 准备见下方与 [`SIGNING.md`](SIGNING.md)

## 要求

- macOS + **Xcode 15+**
- iOS 17+ 模拟器或真机
- 上架 / TestFlight：付费 **Apple Developer Program**

## 获取代码

仓库：https://github.com/coffeeli328/BPB-Worker-Panel  

```bash
git clone https://github.com/coffeeli328/BPB-Worker-Panel.git
cd BPB-Worker-Panel
git fetch origin
git checkout cursor/qimen-appstore-ready-5fa4   # 或 main / 其它功能分支
git pull
```

国内网络不便时：GitHub 对应分支页 **Code → Download ZIP**，或镜像代理后下载。工程路径：`QimenDunjia/QimenDunjia.xcodeproj`。

## 打开与运行

1. 打开 `QimenDunjia.xcodeproj`
2. Target **QimenDunjia** → **Signing & Capabilities**
   - 选择你的 **Team**（必填；仓库内为空）
   - 将 **Bundle ID** 从占位 `com.gglee.QimenDunjia` 改成**你的唯一 ID**
3. 模拟器 iOS 17+ → **Product → Run**（⌘R）
4. 自测：起局（拆补/置闰）→ 盘面 → 解读 → 历史

显示名默认为 **奇门遁甲**。

## 测试（⌘U）

**Product → Test**，或：

```bash
python3 QimenDunjiaTests/verify_golden_cases.py
```

## Archive / TestFlight（简要）

详见 [`SIGNING.md`](SIGNING.md)。摘要：

1. 目的地选 **Any iOS Device (arm64)**（不要用模拟器 Archive）
2. **Product → Archive**
3. Organizer → **Distribute App** → App Store Connect / TestFlight
4. Connect 补齐截图、描述、隐私政策（若需要）

**你必须在 Xcode 自行设置：**

| 项 | 仓库占位 | 你要做的 |
|----|----------|----------|
| Team | （空） | 选 Personal / 公司 Team |
| Bundle Identifier | `com.gglee.QimenDunjia` | 改成全球唯一 |
| 付费账号 | — | TestFlight / 上架需要 |

## 定局口径

起局页可选 **拆补**（默认）或 **置闰**。转盘规则相同。见计划文档。

## 模块一览

| 路径 | 作用 |
|------|------|
| `Engine/` | 定局、排盘、天文、真太阳时、置闰 |
| `Views/` | 起局 / 盘面 / 解读 / 历史 |
| `Assets.xcassets/AppIcon` | 1024 墨色九宫占位图标（可替换） |
| `Info.plist` | 显示名、出口合规声明；**无**定位隐私键 |
| `SIGNING.md` | 签名与上架清单 |

## 口径与残差

见 Agent Store 计划 `docs/qimen-ios-plan.md`（拆补 / 置闰 SOLID vs APPROXIMATE）。
