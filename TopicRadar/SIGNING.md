# Signing & App Store（话题雷达）

上架前请在 **你自己的 Apple 开发者账号** 下完成下列项。仓库内仅为占位。

## 1. Xcode Signing

1. 打开 `TopicRadar.xcodeproj`，选中 target **TopicRadar**
2. **Signing & Capabilities**
   - 勾选 **Automatically manage signing**
   - **Team**：选你的 Personal Team 或付费 Team（当前 `DEVELOPMENT_TEAM` 为空）
3. **Bundle Identifier**
   - 占位：`com.gglee.TopicRadar`
   - **必须改成全球唯一**

## 2. 显示名

- 主屏幕：**话题雷达**（`CFBundleDisplayName`）

## 3. 网络与隐私

- App 需访问互联网以拉取公开 RSS（ATS 默认允许 HTTPS）
- **无定位 / 通讯录 / 相机** 权限
- 数据存本机 SwiftData；无自建账号体系

## 4. 出口合规

- `ITSAppUsesNonExemptEncryption = false`（仅标准 HTTPS）

## 5. Archive

1. 目的地：**Any iOS Device (arm64)**
2. **Product → Archive** → Organizer → **Distribute App**
