//
//  TopicsHomeView.swift
//  TopicRadar
//

import SwiftUI
import SwiftData

struct TopicsHomeView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \RadarTopic.updatedAt, order: .reverse) private var topics: [RadarTopic]

    @State private var isCollecting = false
    @State private var statusMessage = ""
    @State private var showAddTopic = false

    var body: some View {
        NavigationStack {
            ZStack {
                AppTheme.screenBackground

                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        hero
                        topicSection
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 28)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showAddTopic = true
                    } label: {
                        Image(systemName: "plus")
                    }
                    .accessibilityLabel("新建话题")
                }
            }
            .sheet(isPresented: $showAddTopic) {
                AddTopicView()
            }
        }
    }

    private var hero: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("话题雷达")
                .font(AppTheme.brandFont)
                .foregroundStyle(AppTheme.sand)
            Text("把散落在网上的同一话题，收成一条情报流")
                .font(AppTheme.headlineFont)
                .foregroundStyle(AppTheme.sand.opacity(0.95))
                .fixedSize(horizontal: false, vertical: true)
            Text("选定话题、挂上公开 RSS，一键汇集标题、摘要与来源。")
                .font(.subheadline)
                .foregroundStyle(AppTheme.sand.opacity(0.78))

            Button {
                Task { await collectAll() }
            } label: {
                Text(isCollecting ? "采集中…" : "立即采集全部")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(AppTheme.signal)
                    .foregroundStyle(AppTheme.ink)
                    .clipShape(Capsule())
            }
            .disabled(isCollecting || topics.isEmpty)
            .padding(.top, 4)

            if !statusMessage.isEmpty {
                Text(statusMessage)
                    .font(.footnote)
                    .foregroundStyle(AppTheme.sand.opacity(0.8))
            }
        }
        .padding(22)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [AppTheme.ink, AppTheme.sea],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .shadow(color: AppTheme.ink.opacity(0.18), radius: 18, y: 10)
        .padding(.top, 8)
    }

    private var topicSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("我的话题")
                .font(AppTheme.headlineFont)
                .foregroundStyle(AppTheme.ink)
            Text("先选一个话题，再管理源与阅读结果。")
                .font(.subheadline)
                .foregroundStyle(AppTheme.muted)

            if topics.isEmpty {
                Text("暂无话题。点右上角 + 新建，或重启 App 以载入温哥华房价示例。")
                    .foregroundStyle(AppTheme.muted)
                    .padding(.vertical, 12)
            } else {
                ForEach(topics) { topic in
                    NavigationLink(value: topic.id) {
                        TopicCard(topic: topic)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .navigationDestination(for: UUID.self) { id in
            if let topic = topics.first(where: { $0.id == id }) {
                TopicDetailView(topic: topic)
            }
        }
    }

    @MainActor
    private func collectAll() async {
        isCollecting = true
        defer { isCollecting = false }
        var total = CollectSummary()
        for topic in topics {
            let result = await RSSCollector.collect(topic: topic, context: modelContext)
            total.fetched += result.fetched
            total.added += result.added
            total.updated += result.updated
            total.sourceErrors.append(contentsOf: result.sourceErrors)
        }
        statusMessage = total.message
    }
}

struct TopicCard: View {
    let topic: RadarTopic

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(topic.name)
                .font(.title3.weight(.semibold))
                .foregroundStyle(AppTheme.ink)
            Text("\(topic.articles.count) 条 · \(topic.sources.count) 个源")
                .font(.subheadline)
                .foregroundStyle(AppTheme.muted)
            if !topic.topicDescription.isEmpty {
                Text(topic.topicDescription)
                    .font(.footnote)
                    .foregroundStyle(AppTheme.muted)
                    .lineLimit(2)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.white.opacity(0.72))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(AppTheme.line, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}
