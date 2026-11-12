# 八字运势 (BaZiYuCe) — Android

Jetpack Compose 本地八字排盘与流年 / 流月 / 流日规则预测。

- 四柱：年（立春）、月（十二节）、日（儒略日）、时（五鼠遁）
- **多人档案**：新增 / 切换 / 管理，各自排盘
- **日历强弱**：流日格指数 + 流月 / 流年；可浏览过去与未来
- 十神、五行概览、刑冲合害简表 → 可扩展的运势文案
- **中文界面**；无需联网
- 与同仓库 iOS `BaZiYuCe/` **同计算与预测口径**（UI：档案→起盘→八字→日历）

与 `QimenDunjiaAndroid/` 并列；天文节气模块与奇门 / iOS 八字同 Meeus 近似。

## 要求

- Android Studio Hedgehog+（或命令行 SDK）
- JDK 17
- compileSdk / targetSdk **34**，minSdk **24**
- 侧载安装若提示「未安装应用」：先卸载旧版 `com.gglee.baziyuce`，再装完整 APK（见 GitHub Release）

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

1. **档案**：新增人员或切换已有档案  
2. **起盘**：姓名 + 年/月/日/时辰/性别 →「排出八字」  
3. **八字**：查看四柱、日主、五行条  
4. **日历**：查看强弱指数，点日/月/年看详情（左右切换过去/未来）  

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
| `engine/CalendarStrength.kt` | 日历日/月/年强弱指数 |
| `data/PeopleStore.kt` | 多人档案持久化 |
| `ui/` | 档案 / 起盘 / 八字 / 日历 / 关于 |

## 口径

节气为 Meeus 低精度近似；交节前后极短窗口可能与专业历书差一日。详见 App 内「关于」。预测为规则参考，非专业命理结论。
