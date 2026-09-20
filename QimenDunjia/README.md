# 奇门遁甲 (Qimen Dunjia) — iOS

SwiftUI 本地排盘 App：**时家奇门 · 转盘排宫 · 拆补定局**。

- **MVP 状态：基线已合 main**；本分支增加**可选置闰定局**（默认拆补）
- **不含**飞盘法

## 定局口径

起局页可选 **拆补**（默认）或 **置闰**。转盘排盘规则相同，仅局数来源不同。详见计划文档。

## 要求

- macOS + **Xcode 15+**
- iOS 17+ 模拟器或真机

## 获取本分支最新代码

分支：`cursor/qimen-dunjia-ios-5fa4`  
仓库：https://github.com/coffeeli328/BPB-Worker-Panel  
PR：https://github.com/coffeeli328/BPB-Worker-Panel/pull/1

### 方式 A：Git 拉取（推荐）

```bash
git clone https://github.com/coffeeli328/BPB-Worker-Panel.git
cd BPB-Worker-Panel
git fetch origin
git checkout cursor/qimen-dunjia-ios-5fa4
git pull origin cursor/qimen-dunjia-ios-5fa4
```

已有克隆时只需后两行，即可拿到含最新 UI 的提交。

### 方式 B：ZIP（国内网络不便时）

1. 浏览器打开分支页：  
   https://github.com/coffeeli328/BPB-Worker-Panel/tree/cursor/qimen-dunjia-ios-5fa4  
2. 点绿色 **Code → Download ZIP**，或直接：  
   https://github.com/coffeeli328/BPB-Worker-Panel/archive/refs/heads/cursor/qimen-dunjia-ios-5fa4.zip  
3. 解压后进入 `…/QimenDunjia/`，用 Xcode 打开工程。

若 GitHub 访问慢，可用镜像站打开同一路径后下载 ZIP（例如 `ghproxy` / `gitclone` 等对 `github.com` 的代理前缀），或由他人转发该 ZIP。解压后路径仍以 `QimenDunjia/QimenDunjia.xcodeproj` 为准。

## 打开与运行

1. 打开 `QimenDunjia/QimenDunjia.xcodeproj`（或本目录下的 `QimenDunjia.xcodeproj`）
2. 选中 target **QimenDunjia**；Signing 选个人 Team（真机需要）
3. 选 iPhone 模拟器（iOS 17+）→ **Product → Run**（⌘R）
4. 建议自测路径：起局排盘 → 看九宫层次 → 简要解读 → 历史回看

## 测试（⌘U）

在 Xcode：**Product → Test**（⌘U），运行 `QimenDunjiaTests` 黄金/回归用例。

无 Mac 时可在仓库根或本目录执行：

```bash
python3 QimenDunjiaTests/verify_golden_cases.py
```

## 模块一览

| 路径 | 作用 |
|------|------|
| `Engine/JuResolver.swift` | 拆补法定局 |
| `Engine/QimenEngine.swift` | 地盘/天盘/九星/八门/八神/旬空 |
| `Engine/AstronomyCore.swift` | Meeus 视黄经、ΔT、均时差 |
| `Engine/SolarTerms.swift` | 节气交节 |
| `Engine/TrueSolarTime.swift` | 经度 + 真太阳时；城市预设 |
| `Views/` | 起局 / 盘面 / 解读 / 历史 |
| `QimenDunjiaTests/` | 黄金用例 + 节气边界 / 经度回归 |

## 口径与残差

- **SOLID**：拆补转盘排盘、真太阳时改正、Meeus 节气交节（见计划文档）
- **残差**：节气相对精密历约数分钟；未计大气折射；**不做置闰**
- 详细 SOLID vs APPROXIMATE：项目计划 `docs/qimen-ios-plan.md`（Agent Store）
