# Signing & App Store（必做）

上架前请在 **你自己的 Apple 开发者账号** 下完成下列项。仓库内仅为占位。

## 1. Xcode Signing（每次真机 / Archive 都要）

1. 打开 `QimenDunjia.xcodeproj`，选中 target **QimenDunjia**
2. **Signing & Capabilities**
   - 勾选 **Automatically manage signing**
   - **Team**：选你的 Personal Team 或付费 Apple Developer Program Team  
     （当前工程 `DEVELOPMENT_TEAM` 为空，必须由你填写）
3. **Bundle Identifier**  
   - 占位：`com.gglee.QimenDunjia`  
   - **必须改成全球唯一**（例如 `com.<你的名字或公司>.QimenDunjia`），否则无法注册 App ID / 上传
4. 测试 target `QimenDunjiaTests` 的 Bundle ID 会随主 target 派生；若冲突一并改前缀

## 2. 显示名

- 主屏幕显示名：**奇门遁甲**（`CFBundleDisplayName` / `INFOPLIST_KEY_CFBundleDisplayName`）
- 可在 Info 中自行修改，无需改代码

## 3. 隐私权限

- **不请求定位**。地点用城市预设 / 手动经度，故 **未** 加入 `NSLocationWhenInUseUsageDescription` 等。
- 若你以后接入 Core Location，需自行补隐私文案并重新审核说明。

## 4. 出口合规

- Info 中 `ITSAppUsesNonExemptEncryption = false`（仅 HTTPS / 标准加密场景的常见勾选）。
- App Store Connect 提交时若问卷问到加密，按你的实际网络行为勾选；本 App MVP **无后端**。

## 5. Archive → TestFlight / 上架（简要）

1. 真机或 **Any iOS Device (arm64)** 为目的地  
2. 菜单 **Product → Archive**  
3. Organizer 中 **Distribute App**  
   - 内部测试：TestFlight（需付费开发者账号）  
   - 上架：App Store Connect  
4. 在 Connect 填写截图、描述、年龄分级、隐私政策 URL（若审核要求）

付费 Apple Developer Program（年费）为 TestFlight / App Store 所必需；免费 Personal Team 只能装本机调试。
