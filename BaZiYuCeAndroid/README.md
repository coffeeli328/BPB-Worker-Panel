# 八字运势 (BaZiYuCe) — Android

Jetpack Compose 本地八字排盘与流年 / 流月 / 流日规则预测。

- 四柱：年（立春）、月（十二节）、日（儒略日）、时（五鼠遁）
- 十神、五行概览、刑冲合害简表 → 可扩展的运势文案
- **中文界面**；无需联网
- 与同仓库 iOS `BaZiYuCe/` **同计算与预测口径**

与 `QimenDunjiaAndroid/` 并列；天文节气模块与奇门 / iOS 八字同 Meeus 近似。

## 要求

- Android Studio Hedgehog+（或命令行 SDK）
- JDK 17
- compileSdk / targetSdk **34**，minSdk **26**

## 打开与运行

```bash
git clone https://github.com/coffeeli328/BPB-Worker-Panel.git
cd BPB-Worker-Panel/BaZiYuCeAndroid
# Android Studio → Open 本目录
# 或：
./gradlew :app:assembleDebug
```

1. 可将 applicationId `com.gglee.baziyuce` 改为你的唯一 ID  
2. 模拟器 / 真机 API 26+ → Run  

显示名：**八字运势**。

## 自测流程

1. **起盘**：选出生日期时间、性别 →「排出八字」  
2. **八字**：查看四柱、日主、五行条  
3. **运势**：切换流年 / 流月 / 流日，可改参考日期刷新提示  

## 无完整 SDK 时的逻辑校验

```bash
python3 BaZiYuCeAndroid/scripts/verify_golden_cases.py
```

有 SDK 时：

```bash
cd BaZiYuCeAndroid && ./gradlew :app:testDebugUnitTest
```

## 模块

| 路径 | 作用 |
|------|------|
| `engine/GanzhiCalendar.kt` | 四柱与流年/月/日干支 |
| `engine/TenGods.kt` | 十神 |
| `engine/FiveElements.kt` | 五行力量 |
| `engine/BranchRelations.kt` | 刑冲合害 |
| `engine/PredictionEngine.kt` | 规则预测与提示 |
| `ui/` | 起盘 / 八字 / 运势 / 关于 |

## 口径

节气为 Meeus 低精度近似；交节前后极短窗口可能与专业历书差一日。详见 App 内「关于」。预测为规则参考，非专业命理结论。
