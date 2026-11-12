//
//  RootTabView.swift
//  BaZiYuCe
//

import SwiftUI

struct RootTabView: View {
    var body: some View {
        TabView {
            BirthInputView()
                .tabItem { Label("起盘", systemImage: "calendar.badge.clock") }

            ChartResultView()
                .tabItem { Label("八字", systemImage: "square.grid.2x2") }

            PredictionHubView()
                .tabItem { Label("运势", systemImage: "sparkles") }

            FortuneCalendarView()
                .tabItem { Label("日历", systemImage: "calendar") }

            ProfilesView()
                .tabItem { Label("档案", systemImage: "person.2") }
        }
        .tint(AppTheme.celadon)
    }
}
