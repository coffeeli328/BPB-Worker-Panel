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

    private var tz: TimeZone {
        if let s = request.timeZoneSecondsFromGMT {
            return TimeZone(secondsFromGMT: s) ?? .current
        }
        return .current
    }

    var body: some View {
        NavigationStack(path: $path) {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("奇门遁甲")
                        .font(AppTheme.titleFont)
                        .foregroundStyle(AppTheme.ink)
                    Text("时家 · 转盘 · 拆补定局（准确性优先）")
                        .font(.subheadline)
                        .foregroundStyle(AppTheme.muted)

                    Picker("历法", selection: $request.calendarMode) {
                        ForEach(CalendarInputMode.allCases) { Text($0.rawValue).tag($0) }
                    }
                    .pickerStyle(.segmented)

                    DatePicker("日期时间", selection: $request.date, displayedComponents: [.date, .hourAndMinute])
                        .environment(\.locale, Locale(identifier: "zh_CN"))

                    if request.calendarMode == .lunar {
                        Text("农历参考：\(GanzhiCalendar.lunarDescription(for: request.date, timeZone: tz))")
                            .font(.caption)
                            .foregroundStyle(AppTheme.muted)
                    }

                    Picker("起局", selection: $request.method) {
                        ForEach(QimenMethod.allCases) { Text($0.rawValue).tag($0) }
                    }

                    Toggle("东八区 UTC+8", isOn: Binding(
                        get: { request.timeZoneSecondsFromGMT == 8 * 3600 },
                        set: { on in
                            request.timeZoneSecondsFromGMT = on ? 8 * 3600 : nil
                        }
                    ))

                    Picker("地点", selection: $presetId) {
                        ForEach(LocationPreset.all) { p in
                            Text("\(p.name) \(String(format: "%.2f", p.longitude))°E").tag(p.id)
                        }
                    }
                    .onChange(of: presetId) { _, newId in
                        if let p = LocationPreset.all.first(where: { $0.id == newId }) {
                            request.longitude = p.longitude
                            request.locationNote = p.name
                        }
                    }

                    HStack {
                        Text("经度°E")
                        TextField("116.4", value: $request.longitude, format: .number.precision(.fractionLength(2...4)))
                            .keyboardType(.decimalPad)
                            .multilineTextAlignment(.trailing)
                    }

                    Toggle("真太阳时（排时辰）", isOn: $request.useTrueSolarTime)

                    if request.useTrueSolarTime {
                        let adj = TrueSolarTime.adjustedDate(
                            civil: request.date,
                            timeZone: tz,
                            longitudeEastDegrees: request.longitude
                        )
                        Text(String(format: "经度改正 %+.1f 分 · 均时差 %+.1f 分", adj.longitudeMinutes, adj.eotMinutes))
                            .font(.caption)
                            .foregroundStyle(AppTheme.muted)
                    }

                    Button {
                        let result = QimenEngine.generate(request: request)
                        modelContext.insert(HistoryRecord(chart: result))
                        path.append(result)
                    } label: {
                        Text("排盘")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppTheme.pine)
                }
                .padding(20)
            }
            .background(AppTheme.paper.ignoresSafeArea())
            .navigationDestination(for: QimenChart.self) { chart in
                ChartDetailView(chart: chart)
            }
        }
    }
}
