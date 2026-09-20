//
//  InterpretationView.swift
//  QimenDunjia
//

import SwiftUI
import SwiftData

struct InterpretationView: View {
    let chart: QimenChart

    @Environment(\.modelContext) private var modelContext
    @ObservedObject private var settings = AISettingsStore.shared

    @State private var aiText: String = ""
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var didLoadCache = false

    private let client = AIInterpretationClient()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                if chart.hasQuestion {
                    Text("所问：\(chart.question)")
                        .font(.system(size: 17, weight: .semibold, design: .serif))
                        .foregroundStyle(AppTheme.ink)
                        .padding(.bottom, 4)
                    Text("事项：\(chart.questionTopic.rawValue) · 规则模板，仅供参考")
                        .font(.caption)
                        .foregroundStyle(AppTheme.muted)
                        .padding(.bottom, 16)
                } else {
                    Text("尚未填写所问之事。请返回起局页填写后再排盘，以获得针对性解读。")
                        .font(.subheadline)
                        .foregroundStyle(AppTheme.cinnabar)
                        .padding(.bottom, 16)
                }

                sectionHeader("本机规则解读")
                ForEach(chart.interpretations) { item in
                    ruleRow(item)
                }

                aiSection
                    .padding(.top, 20)
            }
            .padding(20)
        }
        .background(AppTheme.screenBackground)
        .navigationTitle(chart.hasQuestion ? "问事解读" : "简要解读")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { loadCachedAIIfNeeded() }
    }

    // MARK: - Rule rows

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(AppTheme.sectionFont)
            .foregroundStyle(AppTheme.muted)
            .padding(.bottom, 8)
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func ruleRow(_ item: InterpretationItem) -> some View {
        HStack(alignment: .top, spacing: 12) {
            RoundedRectangle(cornerRadius: 1)
                .fill(toneColor(item.tone))
                .frame(width: 3)
                .padding(.vertical, 2)

            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 8) {
                    Text(item.title)
                        .font(.system(size: 16, weight: .semibold, design: .serif))
                        .foregroundStyle(AppTheme.ink)
                    Text(toneLabel(item.tone))
                        .font(.system(size: 10, weight: .medium))
                        .foregroundStyle(toneColor(item.tone))
                }
                Text(item.detail)
                    .font(.system(size: 15))
                    .foregroundStyle(AppTheme.muted)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(.vertical, 12)
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(AppTheme.line.opacity(0.7))
                .frame(height: 1)
        }
    }

    // MARK: - AI

    @ViewBuilder
    private var aiSection: some View {
        sectionHeader("AI 解读（可选）")

        Text("将所问与盘面发送到你在「设置」配置的 OpenAI 兼容接口。未配置或失败时，上方规则解读仍可用。")
            .font(.caption)
            .foregroundStyle(AppTheme.muted)
            .padding(.bottom, 12)

        if !settings.isConfigured {
            Text("尚未配置 API Key。请打开「设置」填写 Base URL、模型与密钥后再试。")
                .font(.subheadline)
                .foregroundStyle(AppTheme.cinnabar)
                .padding(.bottom, 10)
            NavigationLink {
                SettingsView()
            } label: {
                Text("前往设置")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(AppTheme.pine)
            }
        } else {
            Button {
                Task { await runAI(force: false) }
            } label: {
                HStack {
                    if isLoading {
                        ProgressView()
                            .tint(AppTheme.paper)
                    }
                    Text(isLoading ? "解读中…" : (aiText.isEmpty ? "AI 解读" : "重新 AI 解读"))
                        .font(.system(size: 16, weight: .semibold, design: .serif))
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .foregroundStyle(AppTheme.paper)
                .background(AppTheme.pine)
            }
            .disabled(isLoading)
            .padding(.bottom, 8)

            if !aiText.isEmpty && !isLoading {
                Button("强制刷新") {
                    Task { await runAI(force: true) }
                }
                .font(.caption)
                .foregroundStyle(AppTheme.muted)
                .padding(.bottom, 8)
            }
        }

        if let errorMessage {
            VStack(alignment: .leading, spacing: 8) {
                Text(errorMessage)
                    .font(.caption)
                    .foregroundStyle(AppTheme.cinnabar)
                if settings.isConfigured {
                    Button("重试") {
                        Task { await runAI(force: true) }
                    }
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(AppTheme.pine)
                }
            }
            .padding(.bottom, 12)
        }

        if !aiText.isEmpty {
            Text(aiText)
                .font(.system(size: 15))
                .foregroundStyle(AppTheme.ink)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 4)
            Text("AI 输出仅供参考，不保证吉凶。")
                .font(.caption2)
                .foregroundStyle(AppTheme.muted)
                .padding(.top, 10)
        }
    }

    private func loadCachedAIIfNeeded() {
        guard !didLoadCache else { return }
        didLoadCache = true
        guard let record = fetchRecord() else { return }
        let key = AIInterpretationPrompt.cacheKey(chart: chart, model: settings.resolvedModel)
        if record.aiReadingCacheKey == key, !record.aiReadingText.isEmpty {
            aiText = record.aiReadingText
        }
    }

    private func fetchRecord() -> HistoryRecord? {
        let chartId = chart.id
        let descriptor = FetchDescriptor<HistoryRecord>(
            predicate: #Predicate { $0.id == chartId }
        )
        return try? modelContext.fetch(descriptor).first
    }

    @MainActor
    private func runAI(force: Bool) async {
        errorMessage = nil
        let model = settings.resolvedModel
        let key = AIInterpretationPrompt.cacheKey(chart: chart, model: model)

        if !force, let record = fetchRecord(),
           record.aiReadingCacheKey == key, !record.aiReadingText.isEmpty {
            aiText = record.aiReadingText
            return
        }

        guard settings.isConfigured else {
            errorMessage = AIInterpretationError.notConfigured.localizedDescription
            return
        }

        isLoading = true
        defer { isLoading = false }

        do {
            let text = try await client.interpret(
                chart: chart,
                config: .init(
                    baseURL: settings.resolvedBaseURL,
                    apiKey: settings.apiKey,
                    model: model
                )
            )
            aiText = text
            if let record = fetchRecord() {
                record.saveAIReading(text: text, cacheKey: key)
                try? modelContext.save()
            }
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription
                ?? error.localizedDescription
        }
    }

    private func toneColor(_ tone: InterpretationItem.Tone) -> Color {
        switch tone {
        case .auspicious: return AppTheme.pine
        case .caution: return AppTheme.cinnabar
        case .neutral: return AppTheme.ink.opacity(0.45)
        }
    }

    private func toneLabel(_ tone: InterpretationItem.Tone) -> String {
        switch tone {
        case .auspicious: return "宜"
        case .caution: return "慎"
        case .neutral: return "述"
        }
    }
}
