# 八字运势 (BaZiYuCe) — iOS

SwiftUI 本地八字排盘与流年 / 流月 / 流日规则预测。

- 四柱：年（立春）、月（十二节）、日（儒略日）、时（五鼠遁）
- 十神、五行概览、刑冲合害简表 → 可扩展的运势文案
- **中文界面**；无需联网

与同仓库 `QimenDunjia/` 并列；天文节气模块自奇门工程复用。

## 要求

- macOS + **Xcode 15+**
- iOS 17+ 模拟器或真机

## 打开与运行

```bash
git clone https://github.com/coffeeli328/BPB-Worker-Panel.git
cd BPB-Worker-Panel
open BaZiYuCe/BaZiYuCe.xcodeproj
```

1. Target **BaZiYuCe** → **Signing & Capabilities** → 选择 Team  
2. 可将 Bundle ID `com.gglee.BaZiYuCe` 改为你的唯一 ID  
3. 模拟器 iOS 17+ → **Product → Run**

显示名：**八字运势**。

## 自测流程

1. **起盘**：选出生日期时间、性别 →「排出八字」  
2. **八字**：查看四柱、日主、五行条  
3. **运势**：切换流年 / 流月 / 流日，可改参考日期刷新提示  

## 无 Xcode 时的逻辑校验

```bash
python3 BaZiYuCe/BaZiYuCeTests/verify_golden_cases.py
```

## 模块

| 路径 | 作用 |
|------|------|
| `Engine/GanzhiCalendar.swift` | 四柱与流年/月/日干支 |
| `Engine/TenGods.swift` | 十神 |
| `Engine/FiveElements.swift` | 五行力量 |
| `Engine/BranchRelations.swift` | 刑冲合害 |
| `Engine/PredictionEngine.swift` | 规则预测与提示 |
| `Views/` | 起盘 / 八字 / 运势 / 关于 |

## 口径

节气为 Meeus 低精度近似；交节前后极短窗口可能与专业历书差一日。详见 App 内「关于」。预测为规则参考，非专业命理结论。
