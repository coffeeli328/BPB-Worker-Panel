//
//  TopicDetailView.swift
//  TopicRadar
//

import SwiftUI
import SwiftData

struct TopicDetailView: View {
    @Environment(\.modelContext) private var modelContext
    @Bindable var topic: RadarTopic

    @State private var query = ""
    @State private var isCollecting = false
    @State private var statusMessage = ""
    @State private var showAddSource = false
    @State private var showSources = false

    private var filteredArticles: [RadarArticle] {
        let base = topic.sortedArticles
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !q.isEmpty else { return base }
        return base.filter {
            $0.title.lowercased().contains(q)
                || $0.summary.lowercased().contains(q)
                || $0.sourceName.lowercased().contains(q)
        }
    }

    var body: some View {
        ZStack {
            AppTheme.screenBackground
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    header
                    searchAndStatus
                    keywords
                    articles
                }
                .padding(20)
            }
        }
        .navigationTitle(topic.name)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItemGroup(placement: .topBarTrailing) {
                Button("源") { showSources = true }
                Button {
                    Task { await collect() }
                } label: {
                    Image(systemName: "arrow.clockwise")
                }
                .disabled(isCollecting)
            }
        }
        .sheet(isPresented: $showSources) {
            SourcesView(topic: topic, showAddSource: $showAddSource)
        }
        .sheet(isPresented: $showAddSource) {
            AddSourceView(topic: topic)
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 8) {
            if !topic.topicDescription.isEmpty {
                Text(topic.topicDescription)
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.muted)
            }
            HStack {
                Text("\(topic.articles.count) 条 · \(topic.sources.filter(\.enabled).count)/\(topic.sources.count) 源启用")
                    .font(.footnote)
                    .foregroundStyle(AppTheme.muted)
                Spacer()
                if let last = topic.lastCollectedAt {
                    Text("上次 \(last.formatted(date: .abbreviated, time: .shortened))")
                        .font(.footnote)
                        .foregroundStyle(AppTheme.muted)
                }
            }
            Button {
                Task { await collect() }
            } label: {
                Text(isCollecting ? "同步中…" : "同步最新")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(AppTheme.ink)
                    .foregroundStyle(AppTheme.sand)
                    .clipShape(Capsule())
            }
            .disabled(isCollecting)
        }
    }

    private var searchAndStatus: some View {
        VStack(alignment: .leading, spacing: 8) {
            TextField("搜索标题 / 摘要 / 来源", text: $query)
                .textFieldStyle(.roundedBorder)
            if !statusMessage.isEmpty {
                Text(statusMessage)
                    .font(.footnote)
                    .foregroundStyle(statusMessage.contains("失败") ? AppTheme.danger : AppTheme.muted)
            }
        }
    }

    private var keywords: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("关键词")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(AppTheme.ink)
            FlowKeywords(keywords: topic.keywords)
        }
    }

    private var articles: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("资讯")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(AppTheme.ink)

            if filteredArticles.isEmpty {
                Text(topic.articles.isEmpty ? "还没有内容。点「同步最新」从 RSS 源拉取。" : "没有匹配的结果。")
                    .foregroundStyle(AppTheme.muted)
                    .padding(.vertical, 8)
            } else {
                ForEach(filteredArticles) { article in
                    ArticleRowView(article: article)
                }
            }
        }
    }

    @MainActor
    private func collect() async {
        isCollecting = true
        defer { isCollecting = false }
        let result = await RSSCollector.collect(topic: topic, context: modelContext)
        statusMessage = result.message
    }
}

struct FlowKeywords: View {
    let keywords: [String]

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(keywords, id: \.self) { keyword in
                    Text(keyword)
                        .font(.caption)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(AppTheme.seaBright.opacity(0.28))
                        .foregroundStyle(AppTheme.sea)
                        .clipShape(Capsule())
                }
            }
        }
    }
}
