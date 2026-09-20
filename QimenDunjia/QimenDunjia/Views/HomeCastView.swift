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

                    Toggle("使用东八区 (UTC+8)", isOn: Binding(
                        get: { request.timeZoneSecondsFromGMT == 8 * 3600 },
                        set: { on in
                            request.timeZoneSecondsFromGMT = on ? 8 * 3600 : nil
                            request.locationNote = on ? "UTC+8" : "系统时区"
                        }
                    ))

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
