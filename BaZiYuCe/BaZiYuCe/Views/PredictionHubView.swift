//
//  PredictionHubView.swift
//  BaZiYuCe
//

import SwiftUI

struct PredictionHubView: View {
    @Environment(BaZiSession.self) private var session
    @State private var period: FortunePeriod = .year

    var body: some View {
        NavigationStack {
            ZStack {
                AppTheme.screenBackground
                if session.chart != nil {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 20) {
                            Picker("时段", selection: $period) {
                                ForEach(FortunePeriod.allCases) { p in
                                    Text(p.displayName).tag(p)
                                }
                            }
                            .pickerStyle(.segmented)

                            labeled("参考日期") {
                                DatePicker(
                                    "参考日期",
                                    selection: Bindable(session).referenceDate,
                                    displayedComponents: [.date]
                                )
                                .environment(\.locale, Locale(identifier: "zh_CN"))
                                .onChange(of: session.referenceDate) { _, _ in
                                    session.refreshForecasts()
                                }
                            }

                            if let f = session.forecast(for: period) {
                                forecastCard(f)
                                    .id(f.period.rawValue + f.flowingPillar.name)
                                    .transition(.opacity.combined(with: .move(edge: .trailing)))
                            }
                        }
                        .padding(20)
                        .padding(.bottom, 36)
                        .animation(.easeInOut(duration: 0.28), value: period)
                    }
                } else {
                    VStack(spacing: 12) {
                        Image(systemName: "sparkles")
                            .font(.system(size: 40))
                            .foregroundStyle(AppTheme.celadonSoft)
                        Text("排盘后可查看流年、流月、流日运势")
                            .foregroundStyle(AppTheme.slate)
                    }
                }
            }
            .navigationTitle("运势")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private func forecastCard(_ f: FortuneForecast) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(f.period.sectionTitle)
                        .font(AppTheme.sectionFont)
                        .foregroundStyle(AppTheme.celadonSoft)
                    Text(f.flowingPillar.name)
                        .font(AppTheme.titleFont)
                        .foregroundStyle(AppTheme.ink)
                    Text("\(f.stemGod.name) · \(f.tone)")
                        .font(AppTheme.headlineFont)
                        .foregroundStyle(AppTheme.celadon)
                }
                Spacer()
                scoreRing(f.score)
            }

            Text(f.summary)
                .font(AppTheme.bodyFont)
                .foregroundStyle(AppTheme.ink)
                .fixedSize(horizontal: false, vertical: true)

            VStack(alignment: .leading, spacing: 10) {
                Text("运势提示")
                    .font(AppTheme.sectionFont)
                    .foregroundStyle(AppTheme.celadonSoft)
                ForEach(Array(f.tips.enumerated()), id: \.offset) { _, tip in
                    HStack(alignment: .top, spacing: 8) {
                        Circle()
                            .fill(AppTheme.accent)
                            .frame(width: 6, height: 6)
                            .padding(.top, 7)
                        Text(tip)
                            .font(AppTheme.bodyFont)
                            .foregroundStyle(AppTheme.ink)
                    }
                }
            }

            DisclosureGroup {
                VStack(alignment: .leading, spacing: 6) {
                    ForEach(f.ruleNotes, id: \.self) { note in
                        Text("· " + note)
                            .font(.footnote)
                            .foregroundStyle(AppTheme.slate)
                    }
                }
                .padding(.top, 6)
            } label: {
                Text("规则说明（可扩展）")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(AppTheme.celadon)
            }
        }
        .padding(18)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(.white.opacity(0.55))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(AppTheme.line, lineWidth: 1)
                )
        )
    }

    private func scoreRing(_ score: Int) -> some View {
        ZStack {
            Circle()
                .stroke(AppTheme.mistDeep, lineWidth: 8)
            Circle()
                .trim(from: 0, to: CGFloat(score) / 100)
                .stroke(AppTheme.celadon, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                .rotationEffect(.degrees(-90))
            VStack(spacing: 0) {
                Text("\(score)")
                    .font(.title2.weight(.semibold).monospacedDigit())
                    .foregroundStyle(AppTheme.ink)
                Text("倾向")
                    .font(.caption2)
                    .foregroundStyle(AppTheme.slate)
            }
        }
        .frame(width: 72, height: 72)
    }

    @ViewBuilder
    private func labeled<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(AppTheme.sectionFont)
                .foregroundStyle(AppTheme.celadonSoft)
            content()
        }
    }
}
