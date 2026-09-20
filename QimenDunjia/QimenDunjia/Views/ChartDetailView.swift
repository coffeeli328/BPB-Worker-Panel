//
//  ChartDetailView.swift
//  QimenDunjia
//

import SwiftUI

struct ChartDetailView: View {
    let chart: QimenChart

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(chart.juTitle)
                        .font(AppTheme.headlineFont)
                    Text("\(chart.solarTermName) · \(chart.method.rawValue)")
                        .font(.subheadline)
                        .foregroundStyle(AppTheme.muted)
                    Text(chart.ganzhiLine)
                        .font(.body.monospaced())
                    Text("值符 \(chart.zhiFuStar.name)（\(chart.zhiFuPalace.name)）· 值使 \(chart.zhiShiGate.displayName)（\(chart.zhiShiPalace.name)）")
                        .font(.subheadline)
                    Text("旬空 \(chart.xunKong.map(\.name).joined())")
                        .font(.caption)
                        .foregroundStyle(AppTheme.muted)
                }

                PalaceGridView(chart: chart)

                NavigationLink("简要解读") {
                    InterpretationView(chart: chart)
                }
                .font(.headline)
                .foregroundStyle(AppTheme.cinnabar)
            }
            .padding(16)
        }
        .background(AppTheme.paper.ignoresSafeArea())
        .navigationTitle("盘面")
        .navigationBarTitleDisplayMode(.inline)
    }
}
