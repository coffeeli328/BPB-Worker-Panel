//
//  SettingsView.swift
//  QimenDunjia
//
//  OpenAI-compatible AI 解读设置。密钥存 Keychain；无自建后端。
//

import SwiftUI

struct SettingsView: View {
    @ObservedObject private var settings = AISettingsStore.shared
    @State private var revealKey = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Text("AI 解读（可选）")
                    .font(AppTheme.headlineFont)
                    .foregroundStyle(AppTheme.ink)
                    .padding(.bottom, 6)
                Text("使用你自己的 OpenAI 兼容接口。密钥仅存本机 Keychain；开启 AI 时，所问之事与盘面会发送到你填写的 Base URL。未配置时仍可用本机规则解读。")
                    .font(.footnote)
                    .foregroundStyle(AppTheme.muted)
                    .padding(.bottom, 20)

                fieldLabel("Base URL")
                TextField(AISettingsStore.placeholderBaseURLDeepSeek, text: $settings.baseURL)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)
                    .textFieldStyle(.roundedBorder)
                    .padding(.bottom, 4)
                Text("示例：\(AISettingsStore.placeholderBaseURLDeepSeek) 或 \(AISettingsStore.placeholderBaseURLOpenAI)")
                    .font(.caption2)
                    .foregroundStyle(AppTheme.muted)
                    .padding(.bottom, 14)

                fieldLabel("模型 Model")
                TextField(AISettingsStore.placeholderModelDeepSeek, text: $settings.model)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .textFieldStyle(.roundedBorder)
                    .padding(.bottom, 4)
                Text("示例：\(AISettingsStore.placeholderModelDeepSeek) 或 \(AISettingsStore.placeholderModelOpenAI)")
                    .font(.caption2)
                    .foregroundStyle(AppTheme.muted)
                    .padding(.bottom, 14)

                fieldLabel("API Key")
                HStack {
                    Group {
                        if revealKey {
                            TextField("sk-…（存 Keychain）", text: $settings.apiKey)
                        } else {
                            SecureField("sk-…（存 Keychain）", text: $settings.apiKey)
                        }
                    }
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .textFieldStyle(.roundedBorder)

                    Button(revealKey ? "隐藏" : "显示") {
                        revealKey.toggle()
                    }
                    .font(.caption)
                    .foregroundStyle(AppTheme.pine)
                }
                .padding(.bottom, 8)

                HStack(spacing: 12) {
                    Button("DeepSeek 预设") { settings.applyDeepSeekPreset() }
                        .font(.caption.weight(.medium))
                    Button("OpenAI 预设") { settings.applyOpenAIPreset() }
                        .font(.caption.weight(.medium))
                    Spacer()
                    if !settings.apiKey.isEmpty {
                        Button("清除密钥", role: .destructive) { settings.clearKey() }
                            .font(.caption)
                    }
                }
                .foregroundStyle(AppTheme.pine)
                .padding(.bottom, 18)

                statusBanner
                    .padding(.bottom, 16)

                Text("隐私")
                    .font(AppTheme.sectionFont)
                    .foregroundStyle(AppTheme.muted)
                    .padding(.bottom, 8)
                Text("• API Key 不进仓库、不上传我们的服务器（本 App 无自建后端）。\n• 点击「AI 解读」时，问题与盘面摘要会发往你配置的第三方 API。\n• 网络失败或未配置时，自动保留本机规则模板解读。")
                    .font(.footnote)
                    .foregroundStyle(AppTheme.muted)
            }
            .padding(20)
        }
        .background(AppTheme.screenBackground)
        .navigationTitle("设置")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func fieldLabel(_ title: String) -> some View {
        Text(title)
            .font(AppTheme.sectionFont)
            .foregroundStyle(AppTheme.muted)
            .padding(.bottom, 8)
    }

    @ViewBuilder
    private var statusBanner: some View {
        let ok = settings.isConfigured
        Text(ok
             ? "已配置：解读页可使用「AI 解读」。"
             : "未配置完整：请至少填写 API Key（URL/模型可先用预设）。")
            .font(.caption)
            .foregroundStyle(ok ? AppTheme.pine : AppTheme.cinnabar)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(10)
            .background(ok ? AppTheme.wash : AppTheme.wash.opacity(0.6))
            .overlay(
                RoundedRectangle(cornerRadius: 4)
                    .stroke(AppTheme.line, lineWidth: 1)
            )
    }
}
