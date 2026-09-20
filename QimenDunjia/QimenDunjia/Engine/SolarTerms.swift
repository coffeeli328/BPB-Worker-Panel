//
//  SolarTerms.swift
//  QimenDunjia
//
//  二十四节气查找（定局用「当前已交节气」）。
//  APPROXIMATE: 使用固定公历月日正午（目标时区），非天文精密时刻。
//  边界日（节气交接前后数小时）局数可能与专业万年历不一致。
//

import Foundation

struct SolarTermInfo: Hashable {
    let name: String
    let index: Int
    let approximateDate: Date
}

enum SolarTerms {

    static let names: [String] = [
        "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
        "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
        "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
        "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    ]

    /// 近似月日（东八区常用平均）
    private static let approxMonthDay: [(Int, Int)] = [
        (1, 6), (1, 20), (2, 4), (2, 19), (3, 6), (3, 21),
        (4, 5), (4, 20), (5, 6), (5, 21), (6, 6), (6, 21),
        (7, 7), (7, 23), (8, 8), (8, 23), (9, 8), (9, 23),
        (10, 8), (10, 23), (11, 7), (11, 22), (12, 7), (12, 22)
    ]

    static func currentTerm(for date: Date, timeZone: TimeZone) -> SolarTermInfo {
        let terms = termsForNearbyYears(around: date, timeZone: timeZone)
        let past = terms.filter { $0.approximateDate <= date }
        return past.last ?? terms[0]
    }

    static func termsForNearbyYears(around date: Date, timeZone: TimeZone) -> [SolarTermInfo] {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        let year = cal.component(.year, from: date)
        var result: [SolarTermInfo] = []
        for y in (year - 1)...(year + 1) {
            result.append(contentsOf: terms(forSolarYear: y, timeZone: timeZone))
        }
        return result.sorted { $0.approximateDate < $1.approximateDate }
    }

    static func terms(forSolarYear year: Int, timeZone: TimeZone) -> [SolarTermInfo] {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        return zip(names.indices, approxMonthDay).map { idx, md in
            var comps = DateComponents()
            comps.year = year
            comps.month = md.0
            comps.day = md.1
            comps.hour = 12
            let date = cal.date(from: comps) ?? Date()
            return SolarTermInfo(name: names[idx], index: idx + 1, approximateDate: date)
        }
    }
}
