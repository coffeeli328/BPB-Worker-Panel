//
//  SourcesView.swift
//  TopicRadar
//

import SwiftUI
import SwiftData

struct SourcesView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @Bindable var topic: RadarTopic
    @Binding var showAddSource: Bool

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(topic.sources) { source in
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                Text(source.name).font(.headline)
                                Spacer()
                                Toggle("", isOn: Binding(
                                    get: { source.enabled },
                                    set: { source.enabled = $0 }
                                ))
                                .labelsHidden()
                            }
                            Text(source.urlString)
                                .font(.caption)
                                .foregroundStyle(AppTheme.muted)
                                .textSelection(.enabled)
                            if let error = source.lastError, !error.isEmpty {
                                Text(error)
                                    .font(.caption)
                                    .foregroundStyle(AppTheme.danger)
                            }
                        }
                        .swipeActions {
                            Button(role: .destructive) {
                                modelContext.delete(source)
                                topic.updatedAt = .now
                                try? modelContext.save()
                            } label: {
                                Label("删除", systemImage: "trash")
                            }
                        }
                    }
                } footer: {
                    Text("推荐公开 RSS / Google News 检索 feed，避免整站爬虫。")
                }
            }
            .navigationTitle("信息源")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("完成") { dismiss() }
                }
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        dismiss()
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                            showAddSource = true
                        }
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
        }
    }
}

struct AddSourceView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @Bindable var topic: RadarTopic

    @State private var name = ""
    @State private var urlString = ""
    @State private var errorMessage = ""

    var body: some View {
        NavigationStack {
            Form {
                Section("RSS 源") {
                    TextField("名称", text: $name)
                    TextField("https://example.com/feed.xml", text: $urlString)
                        .textInputAutocapitalization(.never)
                        .keyboardType(.URL)
                        .autocorrectionDisabled()
                }
                if !errorMessage.isEmpty {
                    Text(errorMessage).foregroundStyle(AppTheme.danger)
                }
            }
            .navigationTitle("添加源")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("添加") { add() }
                }
            }
        }
    }

    private func add() {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedURL = urlString.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedName.isEmpty else {
            errorMessage = "请填写名称"
            return
        }
        guard let url = URL(string: trimmedURL), url.scheme?.hasPrefix("http") == true else {
            errorMessage = "请填写有效的 http(s) RSS 地址"
            return
        }
        let source = RadarSource(name: trimmedName, urlString: trimmedURL)
        source.topic = topic
        modelContext.insert(source)
        topic.updatedAt = .now
        try? modelContext.save()
        dismiss()
    }
}
