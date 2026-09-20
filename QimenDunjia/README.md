# 奇门遁甲 (Qimen Dunjia) — iOS

SwiftUI 本地排盘 App：**时家奇门 · 转盘排宫 · 拆补定局**。MVP 以**排盘准确性**为第一优先。

## 要求

- macOS + **Xcode 15+**
- iOS 17+ 模拟器或真机

## 打开与运行

1. 打开 `QimenDunjia.xcodeproj`
2. 选中 target **QimenDunjia**，Signing 选择个人 Team（如需要）
3. 选 iPhone 模拟器（iOS 17+）→ **Product → Run**（⌘R）
4. 跑黄金用例：**Product → Test**（⌘U），或在仓库内：

```bash
python3 QimenDunjiaTests/verify_golden_cases.py
```

## 模块

| 路径 | 作用 |
|------|------|
| `Engine/JuResolver.swift` | 拆补法定局（符头→三元→局数歌） |
| `Engine/QimenEngine.swift` | 地盘/天盘/九星/八门/八神/旬空 |
| `Engine/AstronomyCore.swift` | Meeus 视黄经、ΔT、均时差、儒略日 |
| `Engine/SolarTerms.swift` | 节气交节时刻（年相关） |
| `Engine/TrueSolarTime.swift` | 经度改正 + 真太阳时；城市预设 |
| `QimenDunjiaTests/` | 黄金用例 + 节气边界/经度回归 |

## 口径说明

口径说明见 `README` 与项目计划中的 **SOLID vs APPROXIMATE**（真太阳时与节气已升级；残差为数分钟量级）。
