//
//  AddTopicView.swift
//  TopicRadar
//

import SwiftUI
import SwiftData

struct AddTopicView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext

    @State private var name = ""
    @State private var keywordsText = ""
    @State private var descriptionText = ""
    @State private var errorMessage = ""

    var body: some View {
        NavigationStack {
            Form {
                Section("话题") {
                    TextField("名称，例如 多伦多租房", text: $name)
                    TextField("简介（可选）", text: $descriptionText)
                }
                Section("关键词（逗号分隔）") {
                    TextField("多伦多, 租房, rent, Toronto", text: $keywordsText, axis: .vertical)
                        .lineLimit(3...6)
                }
                if !errorMessage.isEmpty {
                    Text(errorMessage).foregroundStyle(AppTheme.danger)
                }
            }
            .navigationTitle("新建话题")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("创建") { create() }
                }
            }
        }
    }

    private func create() {
        let keywords = keywordsText
            .split(whereSeparator: { ",，\n".contains($0) })
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }

        guard !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            errorMessage = "请填写话题名称"
            return
        }
        guard !keywords.isEmpty else {
            errorMessage = "请至少填写一个关键词"
            return
        }

        let topic = RadarTopic(
            name: name.trimmingCharacters(in: .whitespacesAndNewlines),
            topicDescription: descriptionText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                ? "跟踪「\(name)」相关公开资讯"
                : descriptionText.trimmingCharacters(in: .whitespacesAndNewlines),
            keywords: keywords
        )
        modelContext.insert(topic)
        try? modelContext.save()
        dismiss()
    }
}
