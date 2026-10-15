//
//  BaZiSession.swift
//  BaZiYuCe
//

import Foundation
import SwiftUI

/// 十二时辰（用于起盘；引擎仍以代表钟点推时柱）。
struct ShiChen: Identifiable, Hashable {
    let branch: EarthlyBranch
    /// 该时辰代表钟点（子时用 23，便于子时换日）
    let representativeHour: Int

    var id: Int { branch.rawValue }

    var displayName: String {
        "\(branch.name)时（\(timeRangeLabel)）"
    }

    var timeRangeLabel: String {
        switch branch {
        case .zi: return "23–1点"
        case .chou: return "1–3点"
        case .yin: return "3–5点"
        case .mao: return "5–7点"
        case .chen: return "7–9点"
        case .si: return "9–11点"
        case .wu: return "11–13点"
        case .wei: return "13–15点"
        case .shen: return "15–17点"
        case .you: return "17–19点"
        case .xu: return "19–21点"
        case .hai: return "21–23点"
        }
    }

    static let all: [ShiChen] = EarthlyBranch.allCases.map { branch in
        let hour: Int
        switch branch {
        case .zi: hour = 23
        case .chou: hour = 1
        case .yin: hour = 3
        case .mao: hour = 5
        case .chen: hour = 7
        case .si: hour = 9
        case .wu: hour = 11
        case .wei: hour = 13
        case .shen: hour = 15
        case .you: hour = 17
        case .xu: hour = 19
        case .hai: hour = 21
        }
        return ShiChen(branch: branch, representativeHour: hour)
    }
}

@Observable
final class BaZiSession {
    static let minYear = 1900
    static let maxYear = 2100

    var birthYear: Int = 1990
    var birthMonth: Int = 5
    var birthDay: Int = 15
    var birthShiChenIndex: Int = EarthlyBranch.si.rawValue // 巳时 9–11
    var gender: Gender = .male

    var birthDate: Date = Calendar.current.date(from: DateComponents(year: 1990, month: 5, day: 15, hour: 9, minute: 0)) ?? Date()
    var chart: BaZiChart?
    var forecasts: [FortuneForecast] = []
    var referenceDate: Date = Date()
    var inputError: String?

    var timeZone: TimeZone { .current }

    var birthShiChen: ShiChen {
        ShiChen.all[max(0, min(11, birthShiChenIndex))]
    }

    var maxDayInMonth: Int {
        daysInMonth(year: birthYear, month: birthMonth)
    }

    func clampBirthDay() {
        birthDay = min(max(1, birthDay), maxDayInMonth)
    }

    @discardableResult
    func validateAndComposeBirthDate() -> Bool {
        inputError = nil
        if birthYear < Self.minYear || birthYear > Self.maxYear {
            inputError = "年份请在 \(Self.minYear)–\(Self.maxYear) 之间"
            return false
        }
        if birthMonth < 1 || birthMonth > 12 {
            inputError = "月份请在 1–12 之间"
            return false
        }
        let maxDay = daysInMonth(year: birthYear, month: birthMonth)
        if birthDay < 1 || birthDay > maxDay {
            inputError = "\(birthYear)年\(birthMonth)月只有 \(maxDay) 天"
            return false
        }
        if birthShiChenIndex < 0 || birthShiChenIndex > 11 {
            inputError = "请选择有效时辰"
            return false
        }

        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        let comps = DateComponents(
            year: birthYear,
            month: birthMonth,
            day: birthDay,
            hour: birthShiChen.representativeHour,
            minute: 0
        )
        guard let date = cal.date(from: comps) else {
            inputError = "无法识别该出生时间，请检查年月日"
            return false
        }
        birthDate = date
        return true
    }

    func calculate() {
        guard validateAndComposeBirthDate() else { return }
        let c = BaZiEngine.chart(birthDate: birthDate, gender: gender, timeZone: timeZone)
        chart = c
        forecasts = PredictionEngine.allForecasts(chart: c, at: referenceDate, timeZone: timeZone)
    }

    func refreshForecasts() {
        guard let chart else { return }
        forecasts = PredictionEngine.allForecasts(chart: chart, at: referenceDate, timeZone: timeZone)
    }

    func forecast(for period: FortunePeriod) -> FortuneForecast? {
        forecasts.first { $0.period == period }
    }

    private func daysInMonth(year: Int, month: Int) -> Int {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        var comps = DateComponents(year: year, month: month)
        comps.day = 1
        guard let date = cal.date(from: comps),
              let range = cal.range(of: .day, in: .month, for: date)
        else { return 28 }
        return range.count
    }
}
