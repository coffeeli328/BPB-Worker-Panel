//
//  RootTabView.swift
//  BaZiYuCe
//

import SwiftUI

struct RootTabView: View {
    @Environment(BaZiSession.self) private var session

    var body: some View {
        TabView {
            BirthInputView()
                .tabItem { Label("起盘", systemImage: "calendar.badge.clock") }

            ChartResultView()
                .tabItem { Label("八字", systemImage: "square.grid.2x2") }
                .disabled(session.chart == nil)

            PredictionHubView()
                .tabItem { Label("运势", systemImage: "sparkles") }
                .disabled(session.chart == nil)

            AboutView()
                .tabItem { Label("关于", systemImage: "info.circle") }
        }
        .tint(AppTheme.celadon)
    }
}
