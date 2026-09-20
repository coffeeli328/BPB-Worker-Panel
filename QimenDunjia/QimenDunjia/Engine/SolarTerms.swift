//
//  SolarTerms.swift
//  QimenDunjia
//
//  二十四节气：Meeus 视黄经 + 牛顿求根，TT→UT（ΔT）。
//  SOLID（相对旧「正午月日表」）：年相关交节时刻，可用于边界日定局。
//  RESIDUAL: 低精度黄经式 + 近似 ΔT；1900–2100 相对精密历通常约数分钟。
//            交节前后数分钟内与专业历书仍可能差一天定局（极罕见）。
//

import Foundation

struct SolarTermInfo: Hashable {
    let name: String
    let index: Int
    /// 交节绝对时刻（UT 对应的 Date）
    let approximateDate: Date
    /// 目标视黄经（度）
    let longitudeDegrees: Double
}

enum SolarTerms {

    static let names: [String] = [
        "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
        "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
        "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
        "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    ]

    /// 各节气太阳视黄经（度）。小寒=285° … 冬至=270°。
    static let longitudes: [Double] = [
        285, 300, 315, 330, 345, 0,
        15, 30, 45, 60, 75, 90,
        105, 120, 135, 150, 165, 180,
        195, 210, 225, 240, 255, 270
    ]

    static func currentTerm(for date: Date, timeZone: TimeZone) -> SolarTermInfo {
        _ = timeZone // 节气为全球同一瞬间；与时区无关，仅用于接口兼容
        let terms = termsForNearbyYears(around: date)
        let past = terms.filter { $0.approximateDate <= date }
        return past.last ?? terms[0]
    }

    static func termsForNearbyYears(around date: Date) -> [SolarTermInfo] {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(secondsFromGMT: 0)!
        let year = cal.component(.year, from: date)
        var result: [SolarTermInfo] = []
        for y in (year - 1)...(year + 1) {
            result.append(contentsOf: terms(forSolarYear: y))
        }
        return result.sorted { $0.approximateDate < $1.approximateDate }
    }

    /// 指定公历年的 24 节气（该年 1 月小寒 … 12 月冬至）。
    static func terms(forSolarYear year: Int) -> [SolarTermInfo] {
        zip(names.indices, longitudes).map { idx, lon in
            let date = instant(year: year, termIndex: idx)
            return SolarTermInfo(name: names[idx], index: idx + 1, approximateDate: date, longitudeDegrees: lon)
        }
    }

    /// 交节 UT Date。
    static func instant(year: Int, termIndex: Int) -> Date {
        let lon = longitudes[termIndex]
        // 初值：年初 + 约 5+15.2×index 日
        let guessDay = 5.0 + Double(termIndex) * 15.2184
        let guessJD = AstronomyCore.julianDay(year: year, month: 1, day: 1, hourUT: 0) + guessDay
        let jde = AstronomyCore.solveSolarLongitude(targetDegrees: lon, jdeGuess: guessJD)
        let dt = AstronomyCore.deltaTSeconds(year: Double(year))
        let jdUT = jde - dt / 86400.0
        return AstronomyCore.date(fromJulianDayUT: jdUT)
    }
}
