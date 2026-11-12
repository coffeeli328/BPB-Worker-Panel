//
//  FortuneCalendarView.swift
//  BaZiYuCe
//
//  月历展示每日流日强弱指数。
//

import SwiftUI

struct FortuneCalendarView: View {
    @Environment(BaZiSession.self) private var session
    @State private var selectedDay: DayStrength?

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 6), count: 7)
    private let weekdayLabels = ["日", "一", "二", "三", "四", "五", "六"]

    var body: some View {
        NavigationStack {
            ZStack {
                AppTheme.screenBackground
                if session.chart == nil {
                    emptyState
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 18) {
                            profileHint
                            monthHeader
                            weekdayHeader
                            dayGrid
                            legend
                            if let day = selectedDay {
                                dayDetail(day)
                                    .transition(.opacity.combined(with: .move(edge: .bottom)))
                            } else {
                                Text("点选日期查看当日强弱与干支")
                                    .font(.footnote)
                                    .foregroundStyle(AppTheme.slate)
                            }
                        }
                        .padding(20)
                        .padding(.bottom, 36)
                        .animation(.easeInOut(duration: 0.25), value: selectedDay?.id)
                    }
                    .onAppear { session.refreshMonthStrength() }
                }
            }
            .navigationTitle("日历")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "calendar")
                .font(.system(size: 40))
                .foregroundStyle(AppTheme.celadonSoft)
            Text("请先为档案排盘，再查看日历强弱")
                .foregroundStyle(AppTheme.slate)
        }
    }

    private var profileHint: some View {
        Text(session.activeProfile.map { "档案：\($0.name)" } ?? "当前草稿")
            .font(AppTheme.sectionFont)
            .foregroundStyle(AppTheme.celadonSoft)
    }

    private var monthHeader: some View {
        HStack {
            Button {
                session.shiftCalendarMonth(by: -1)
                selectedDay = nil
            } label: {
                Image(systemName: "chevron.left")
                    .foregroundStyle(AppTheme.celadon)
                    .padding(8)
            }
            Spacer()
            Text("\(session.calendarYear)年\(session.calendarMonth)月")
                .font(AppTheme.headlineFont)
                .foregroundStyle(AppTheme.ink)
            Spacer()
            Button {
                session.shiftCalendarMonth(by: 1)
                selectedDay = nil
            } label: {
                Image(systemName: "chevron.right")
                    .foregroundStyle(AppTheme.celadon)
                    .padding(8)
            }
        }
    }

    private var weekdayHeader: some View {
        LazyVGrid(columns: columns, spacing: 6) {
            ForEach(weekdayLabels, id: \.self) { w in
                Text(w)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(AppTheme.slate)
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private var dayGrid: some View {
        let leading = leadingBlankCount()
        return LazyVGrid(columns: columns, spacing: 6) {
            ForEach(0..<leading, id: \.self) { _ in
                Color.clear.frame(height: 52)
            }
            ForEach(session.monthStrength) { day in
                dayCell(day)
            }
        }
    }

    private func dayCell(_ day: DayStrength) -> some View {
        let dayNum = Calendar.current.component(.day, from: day.date)
        let selected = selectedDay?.date == day.date
        return Button {
            selectedDay = day
            session.selectCalendarDay(day.date)
        } label: {
            VStack(spacing: 2) {
                Text("\(dayNum)")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(AppTheme.ink)
                Text("\(day.score)")
                    .font(.caption2.monospacedDigit())
                    .foregroundStyle(strengthColor(day.score))
            }
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(cellFill(score: day.score, selected: selected))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(selected ? AppTheme.celadon : AppTheme.line, lineWidth: selected ? 1.5 : 1)
            )
        }
        .buttonStyle(.plain)
    }

    private func dayDetail(_ day: DayStrength) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("\(format(day.date)) · \(day.flowingDay.name)")
                .font(AppTheme.headlineFont)
                .foregroundStyle(AppTheme.ink)
            Text("强弱指数 \(day.score) · \(day.tone)")
                .font(AppTheme.bodyFont)
                .foregroundStyle(AppTheme.celadon)
            Text("指数来自流日十神与原局刑冲合害规则，与「运势」页同一套引擎。")
                .font(.footnote)
                .foregroundStyle(AppTheme.slate)
            NavigationLink("在运势页查看详情") {
                PredictionHubView()
            }
            .font(.subheadline.weight(.medium))
            .foregroundStyle(AppTheme.celadon)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(.white.opacity(0.55))
        )
    }

    private var legend: some View {
        HStack(spacing: 12) {
            legendSwatch("偏弱", Color(red: 0.55, green: 0.28, blue: 0.26).opacity(0.18))
            legendSwatch("平稳", AppTheme.accent.opacity(0.18))
            legendSwatch("偏强", AppTheme.celadon.opacity(0.22))
        }
        .font(.caption)
        .foregroundStyle(AppTheme.slate)
    }

    private func legendSwatch(_ title: String, _ color: Color) -> some View {
        HStack(spacing: 6) {
            RoundedRectangle(cornerRadius: 4)
                .fill(color)
                .frame(width: 16, height: 16)
            Text(title)
        }
    }

    private func leadingBlankCount() -> Int {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = session.timeZone
        guard let first = cal.date(from: DateComponents(year: session.calendarYear, month: session.calendarMonth, day: 1)) else {
            return 0
        }
        // 周日=1 … 周六=7 → 转换为 0…6 且以周日开头
        return cal.component(.weekday, from: first) - 1
    }

    private func cellFill(score: Int, selected: Bool) -> Color {
        let base: Color
        switch score {
        case 75...: base = AppTheme.celadon.opacity(0.22)
        case 55..<75: base = AppTheme.accent.opacity(0.16)
        default: base = Color(red: 0.55, green: 0.28, blue: 0.26).opacity(0.14)
        }
        return selected ? AppTheme.celadon.opacity(0.28) : base
    }

    private func strengthColor(_ score: Int) -> Color {
        switch score {
        case 75...: return AppTheme.celadon
        case 55..<75: return AppTheme.accent
        default: return Color(red: 0.55, green: 0.28, blue: 0.26)
        }
    }

    private func format(_ date: Date) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "zh_CN")
        f.dateFormat = "M月d日 EEEE"
        return f.string(from: date)
    }
}
