//
//  ChartDetailView.swift
//  QimenDunjia
//

import SwiftUI

struct ChartDetailView: View {
    let chart: QimenChart

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                headerBlock
                PalaceGridView(chart: chart)
                NavigationLink {
                    InterpretationView(chart: chart)
                } label: {
                    HStack {
                        Text(chart.hasQuestion ? "针对所问解读" : "简要解读")
                            .font(.system(size: 16, weight: .semibold, design: .serif))
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.caption.weight(.semibold))
                    }
                    .foregroundStyle(AppTheme.cinnabar)
                    .padding(.vertical, 12)
                    .padding(.horizontal, 4)
                    .overlay(alignment: .bottom) {
                        Rectangle()
                            .fill(AppTheme.line)
                            .frame(height: 1)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
        .background(AppTheme.screenBackground)
        .navigationTitle("盘面")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var headerBlock: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(chart.juTitle)
                .font(AppTheme.headlineFont)
                .foregroundStyle(AppTheme.ink)

            Text("\(chart.solarTermName) · \(chart.yuanName) · \(chart.juMethod.rawValue) · \(chart.juPhase.rawValue)")
                .font(.subheadline)
                .foregroundStyle(AppTheme.muted)

            Text(chart.ganzhiLine)
                .font(.system(.body, design: .serif))
                .foregroundStyle(AppTheme.ink)
                .padding(.top, 2)

            if chart.hasQuestion {
                Text("所问：\(chart.question)")
                    .font(.system(size: 15, weight: .medium, design: .serif))
                    .foregroundStyle(AppTheme.pine)
                Text("用神侧重：\(chart.questionTopic.rawValue)")
                    .font(.caption)
                    .foregroundStyle(AppTheme.muted)
            } else {
                Text("未填写所问之事 — 解读将提醒补问")
                    .font(.caption)
                    .foregroundStyle(AppTheme.cinnabar)
            }

            HStack(spacing: 16) {
                labeled("值符", "\(chart.zhiFuStar.name)·\(chart.zhiFuPalace.name)")
                labeled("值使", "\(chart.zhiShiGate.displayName)·\(chart.zhiShiPalace.name)")
                labeled("旬空", chart.xunKong.map(\.name).joined())
            }
            .padding(.top, 4)

            if chart.usedTrueSolarTime {
                Text(String(
                    format: "%@ · 东经%.2f° · 真太阳时（经度%+.1f分 均时差%+.1f分）",
                    chart.locationNote,
                    chart.longitude,
                    chart.longitudeCorrectionMinutes,
                    chart.equationOfTimeMinutes
                ))
                .font(.caption)
                .foregroundStyle(AppTheme.muted)
                .padding(.top, 2)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func labeled(_ title: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(AppTheme.sectionFont)
                .foregroundStyle(AppTheme.muted)
            Text(value)
                .font(.system(size: 14, weight: .medium, design: .serif))
                .foregroundStyle(AppTheme.ink)
                .lineLimit(1)
                .minimumScaleFactor(0.75)
        }
    }
}
