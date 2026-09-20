//
//  HomeCastView.swift
//  QimenDunjia
//

import SwiftUI
import SwiftData

struct HomeCastView: View {
    @Environment(\.modelContext) private var modelContext
    @State private var request = ChartRequest()
    @State private var path = NavigationPath()
    @State private var presetId: String = "beijing"
    @State private var isCasting = false

    private var tz: TimeZone {
        if let s = request.timeZoneSecondsFromGMT {
            return TimeZone(secondsFromGMT: s) ?? .current
        }
        return .current
    }

    var body: some View {
        NavigationStack(path: $path) {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    brandHeader
                        .padding(.bottom, 24)

                    sectionLabel("时刻")
                    VStack(alignment: .leading, spacing: 14) {
                        Picker("历法", selection: $request.calendarMode) {
                            ForEach(CalendarInputMode.allCases) { Text($0.rawValue).tag($0) }
                        }
                        .pickerStyle(.segmented)

                        DatePicker(
                            "日期时间",
                            selection: $request.date,
                            displayedComponents: [.date, .hourAndMinute]
                        )
                        .environment(\.locale, Locale(identifier: "zh_CN"))
                        .tint(AppTheme.pine)

                        if request.calendarMode == .lunar {
                            Text("农历参考：\(GanzhiCalendar.lunarDescription(for: request.date, timeZone: tz))")
                                .font(.caption)
                                .foregroundStyle(AppTheme.muted)
                        }
                    }
                    .padding(.bottom, 22)

                    sectionLabel("地点与时制")
                    VStack(alignment: .leading, spacing: 14) {
                        Toggle("东八区 UTC+8", isOn: Binding(
                            get: { request.timeZoneSecondsFromGMT == 8 * 3600 },
                            set: { on in
                                request.timeZoneSecondsFromGMT = on ? 8 * 3600 : nil
                            }
                        ))
                        .tint(AppTheme.pine)

                        Picker("地点", selection: $presetId) {
                            ForEach(LocationPreset.all) { p in
                                Text("\(p.name)  \(String(format: "%.1f", p.longitude))°E").tag(p.id)
                            }
                        }

                        HStack {
                            Text("经度 °E")
                                .foregroundStyle(AppTheme.ink)
                            Spacer()
                            TextField("116.4", value: $request.longitude, format: .number.precision(.fractionLength(2...4)))
                                .keyboardType(.decimalPad)
                                .multilineTextAlignment(.trailing)
                                .frame(maxWidth: 120)
                        }
                        .font(.body)

                        Toggle("真太阳时（排时辰）", isOn: $request.useTrueSolarTime)
                            .tint(AppTheme.pine)

                        if request.useTrueSolarTime {
                            let adj = TrueSolarTime.adjustedDate(
                                civil: request.date,
                                timeZone: tz,
                                longitudeEastDegrees: request.longitude
                            )
                            Text(String(
                                format: "经度改正 %+.1f 分 · 均时差 %+.1f 分",
                                adj.longitudeMinutes,
                                adj.eotMinutes
                            ))
                            .font(.caption)
                            .foregroundStyle(AppTheme.muted)
                        }
                    }
                    .padding(.bottom, 22)

                    sectionLabel("起局")
                    VStack(alignment: .leading, spacing: 12) {
                        Text(request.method.rawValue)
                            .font(.system(size: 16, weight: .medium, design: .serif))
                            .foregroundStyle(AppTheme.ink)

                        Picker("定局", selection: $request.juMethod) {
                            ForEach(JuMethod.allCases) { m in
                                Text(m.rawValue).tag(m)
                            }
                        }
                        .pickerStyle(.segmented)

                        Text(request.juMethod.detail)
                            .font(.caption)
                            .foregroundStyle(AppTheme.muted)
                    }
                    .padding(.bottom, 20)

                    Button {
                        castChart()
                    } label: {
                        HStack {
                            if isCasting {
                                ProgressView()
                                    .tint(AppTheme.paper)
                            }
                            Text(isCasting ? "排盘中…" : "起局排盘")
                                .font(.system(size: 17, weight: .semibold, design: .serif))
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 15)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppTheme.pine)
                    .disabled(isCasting)
                    .padding(.bottom, 8)

                    Text("结果将写入本地历史，可随时回看。")
                        .font(.caption)
                        .foregroundStyle(AppTheme.muted)
                }
                .padding(20)
            }
            .background(AppTheme.screenBackground)
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(for: QimenChart.self) { chart in
                ChartDetailView(chart: chart)
            }
            .onChange(of: presetId) { _, newId in
                if let p = LocationPreset.all.first(where: { $0.id == newId }) {
                    request.longitude = p.longitude
                    request.locationNote = p.name
                }
            }
        }
    }

    private var brandHeader: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("奇门遁甲")
                .font(AppTheme.titleFont)
                .foregroundStyle(AppTheme.ink)
            Text("时家 · 转盘 · 拆补定局")
                .font(.system(size: 14, weight: .regular, design: .serif))
                .foregroundStyle(AppTheme.muted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
    }

    private func sectionLabel(_ title: String) -> some View {
        Text(title)
            .font(AppTheme.sectionFont)
            .foregroundStyle(AppTheme.muted)
            .padding(.bottom, 10)
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func castChart() {
        isCasting = true
        // Brief feedback so CTA feels intentional; engine is sync/local.
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.18) {
            let result = QimenEngine.generate(request: request)
            modelContext.insert(HistoryRecord(chart: result))
            isCasting = false
            path.append(result)
        }
    }
}
