//
//  PredictionHubView.swift
//  BaZiYuCe
//

import SwiftUI

struct PredictionHubView: View {
    @Environment(BaZiSession.self) private var session
    @State private var period: FortunePeriod = .year
    @State private var direction: TimelineDirection = .present
    @State private var selectedTimeline: FortuneForecast?

    var body: some View {
        NavigationStack {
            ZStack {
                AppTheme.screenBackground
                if session.chart != nil {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 20) {
                            if let name = session.activeProfile?.name {
                                Text("档案：\(name)")
                                    .font(AppTheme.sectionFont)
                                    .foregroundStyle(AppTheme.celadonSoft)
                            }

                            Picker("时段", selection: $period) {
                                ForEach(FortunePeriod.allCases) { p in
                                    Text(p.displayName).tag(p)
                                }
                            }
                            .pickerStyle(.segmented)

                            Picker("浏览", selection: $direction) {
                                ForEach(TimelineDirection.allCases) { d in
                                    Text(d.displayName).tag(d)
                                }
                            }
                            .pickerStyle(.segmented)
                            .onChange(of: direction) { _, _ in selectedTimeline = nil }
                            .onChange(of: period) { _, _ in selectedTimeline = nil }

                            labeled("参考锚点") {
                                DatePicker(
                                    "参考日期",
                                    selection: Bindable(session).referenceDate,
                                    displayedComponents: [.date]
                                )
                                .environment(\.locale, Locale(identifier: "zh_CN"))
                                .onChange(of: session.referenceDate) { _, _ in
                                    session.refreshForecasts()
                                    selectedTimeline = nil
                                }
                            }

                            Text(browseHint)
                                .font(.footnote)
                                .foregroundStyle(AppTheme.slate)

                            if direction == .present {
                                if let f = session.forecast(for: period) {
                                    ForecastCardView(forecast: f, showDate: true)
                                        .id(f.period.rawValue + f.flowingPillar.name + "\(f.score)")
                                        .transition(.opacity.combined(with: .move(edge: .trailing)))
                                }
                            } else {
                                timelineList
                                if let selectedTimeline {
                                    ForecastCardView(forecast: selectedTimeline, showDate: true)
                                        .transition(.opacity)
                                }
                            }
                        }
                        .padding(20)
                        .padding(.bottom, 36)
                        .animation(.easeInOut(duration: 0.28), value: period)
                        .animation(.easeInOut(duration: 0.28), value: direction)
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

    private var browseHint: String {
        switch direction {
        case .present:
            return "「当下」显示参考锚点对应的流\(period.displayName.suffix(1))详解。"
        case .past:
            return "「往昔」浏览参考锚点之前的流\(period.displayName.suffix(1))强弱列表，点选可看详情。"
        case .future:
            return "「前瞻」浏览参考锚点之后的流\(period.displayName.suffix(1))预测列表，点选可看详情。"
        }
    }

    private var timelineList: some View {
        let items = session.timeline(period: period, direction: direction, count: period == .day ? 14 : 12)
        return VStack(alignment: .leading, spacing: 10) {
            Text(direction == .past ? "往昔列表" : "前瞻列表")
                .font(AppTheme.sectionFont)
                .foregroundStyle(AppTheme.celadonSoft)
            ForEach(items) { item in
                Button {
                    selectedTimeline = item
                    session.referenceDate = item.targetDate
                    session.refreshForecasts()
                } label: {
                    TimelineRowView(forecast: item)
                }
                .buttonStyle(.plain)
            }
        }
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
