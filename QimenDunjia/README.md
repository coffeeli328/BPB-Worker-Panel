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
| `Engine/GanzhiCalendar.swift` | 四柱干支 |
| `Engine/SolarTerms.swift` | 节气（近似表） |
| `QimenDunjiaTests/` | 黄金用例（阳遁一局伏吟、丁卯、六局乙巳、阴九丙寅等） |

## 口径说明

见工程内代码注释与项目计划文档中的 **Verified vs Approximate** 列表。重要用事请与专业历书交叉验证。
