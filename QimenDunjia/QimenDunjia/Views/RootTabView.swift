//
//  RootTabView.swift
//  QimenDunjia
//

import SwiftUI

struct RootTabView: View {
    var body: some View {
        TabView {
            HomeCastView()
                .tabItem { Label("起局", systemImage: "circle.grid.3x3") }
            HistoryListView()
                .tabItem { Label("历史", systemImage: "clock") }
        }
        .tint(AppTheme.cinnabar)
    }
}
