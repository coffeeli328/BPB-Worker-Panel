//
//  ChartResultView.swift
//  BaZiYuCe
//

import SwiftUI

struct ChartResultView: View {
    @Environment(BaZiSession.self) private var session

    var body: some View {
        NavigationStack {
            ZStack {
                AppTheme.screenBackground
                if let chart = session.chart {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 24) {
                            header(chart)
                            pillarsRow(chart)
                            dayMasterBlock(chart)
                            fiveElementsBlock(chart)
                            noteBlock(chart)
                        }
                        .padding(20)
                        .padding(.bottom, 32)
                    }
                } else {
                    emptyState
                }
            }
            .navigationTitle("八字")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "calendar")
                .font(.system(size: 40))
                .foregroundStyle(AppTheme.celadonSoft)
            Text("请先在「起盘」输入出生时间")
                .foregroundStyle(AppTheme.slate)
        }
    }

    private func header(_ chart: BaZiChart) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(chart.gender.displayName + "命")
                .font(AppTheme.sectionFont)
                .foregroundStyle(AppTheme.celadon)
            Text(formatted(chart.birthDate))
                .font(AppTheme.titleFont)
                .foregroundStyle(AppTheme.ink)
        }
    }

    private func pillarsRow(_ chart: BaZiChart) -> some View {
        HStack(spacing: 10) {
            ForEach(chart.pillars) { p in
                VStack(spacing: 8) {
                    Text(p.kind.displayName)
                        .font(.caption)
                        .foregroundStyle(AppTheme.slate)
                    Text(p.stemBranch.stem.name)
                        .font(AppTheme.pillarFont)
                        .foregroundStyle(AppTheme.ink)
                    Text(p.stemBranch.branch.name)
                        .font(AppTheme.pillarFont)
                        .foregroundStyle(AppTheme.celadon)
                    if let god = p.stemGod {
                        Text(god.name)
                            .font(.caption2)
                            .foregroundStyle(AppTheme.accent)
                    } else {
                        Text("日主")
                            .font(.caption2)
                            .foregroundStyle(AppTheme.accent)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(.white.opacity(0.5))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .stroke(AppTheme.line, lineWidth: 1)
                        )
                )
            }
        }
    }

    private func dayMasterBlock(_ chart: BaZiChart) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("日主")
                .font(AppTheme.sectionFont)
                .foregroundStyle(AppTheme.celadonSoft)
            Text("\(chart.dayMaster.name)（\(chart.dayMaster.yinYang.name)\(chart.dayMaster.wuXing.name)）")
                .font(AppTheme.headlineFont)
                .foregroundStyle(AppTheme.ink)
            Text("十神均以日主为基准推算；日支藏干主气为「\(TenGods.ofBranchMain(branch: chart.day.branch, dayMaster: chart.dayMaster).name)」。")
                .font(AppTheme.bodyFont)
                .foregroundStyle(AppTheme.slate)
        }
    }

    private func fiveElementsBlock(_ chart: BaZiChart) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("五行概览")
                .font(AppTheme.sectionFont)
                .foregroundStyle(AppTheme.celadonSoft)
            Text(chart.balance.summary)
                .font(AppTheme.bodyFont)
                .foregroundStyle(AppTheme.ink)

            ForEach(WuXing.allCases) { wx in
                let value = chart.balance.score(wx)
                let maxV = WuXing.allCases.map { chart.balance.score($0) }.max() ?? 1
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(wx.name)
                            .font(.subheadline.weight(.medium))
                        Spacer()
                        Text(String(format: "%.1f", value))
                            .font(.caption.monospacedDigit())
                            .foregroundStyle(AppTheme.slate)
                    }
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            Capsule().fill(AppTheme.mistDeep.opacity(0.5))
                            Capsule()
                                .fill(AppTheme.celadonSoft.opacity(0.85))
                                .frame(width: max(8, geo.size.width * CGFloat(value / max(maxV, 0.1))))
                        }
                    }
                    .frame(height: 8)
                }
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(.white.opacity(0.45))
        )
    }

    private func noteBlock(_ chart: BaZiChart) -> some View {
        Text(chart.solarTermNote)
            .font(.footnote)
            .foregroundStyle(AppTheme.slate)
    }

    private func formatted(_ date: Date) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "zh_CN")
        f.dateFormat = "yyyy年M月d日 HH:mm"
        return f.string(from: date)
    }
}
