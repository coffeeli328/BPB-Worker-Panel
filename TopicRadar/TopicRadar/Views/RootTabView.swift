//
//  RootTabView.swift
//  TopicRadar
//

import SwiftUI
import SwiftData

struct RootTabView: View {
    @Environment(\.modelContext) private var modelContext

    var body: some View {
        TabView {
            TopicsHomeView()
                .tabItem {
                    Label("话题", systemImage: "dot.radiowaves.left.and.right")
                }

            AboutView()
                .tabItem {
                    Label("关于", systemImage: "info.circle")
                }
        }
        .tint(AppTheme.sea)
        .task {
            try? SeedData.ensureSeedIfNeeded(context: modelContext)
        }
    }
}

struct AboutView: View {
    var body: some View {
        NavigationStack {
            ZStack {
                AppTheme.screenBackground
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        Text("话题雷达")
                            .font(AppTheme.titleFont)
                            .foregroundStyle(AppTheme.ink)
                        Text("在 iPhone 上把公开 RSS 源聚合成话题情报流。默认示例：温哥华房价。")
                            .font(AppTheme.bodyFont)
                            .foregroundStyle(AppTheme.muted)
                        Text("优先使用媒体 / Google News / 社区的公开 feed，数据保存在本机 SwiftData，无需自建服务器。")
                            .font(AppTheme.bodyFont)
                            .foregroundStyle(AppTheme.muted)
                        Text("打开工程：TopicRadar/TopicRadar.xcodeproj。Signing 说明见 SIGNING.md。")
                            .font(.footnote)
                            .foregroundStyle(AppTheme.muted)
                    }
                    .padding(20)
                }
            }
            .navigationTitle("关于")
        }
    }
}
