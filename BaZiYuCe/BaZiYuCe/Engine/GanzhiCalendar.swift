//
//  GanzhiCalendar.swift
//  BaZiYuCe
//
//  四柱干支。年柱立春、月柱十二节；日柱儒略日；时柱五鼠遁。
//  SOLID: 日柱子时换日、五鼠遁、五虎遁。
//  APPROXIMATE: 节气用 Meeus 低精度黄经（见 SolarTerms）。
//

import Foundation

enum GanzhiCalendar {

    /// 排出四柱（本地时区墙钟时间）。
    static func stemBranchFourPillars(for date: Date, timeZone: TimeZone) -> (year: StemBranch, month: StemBranch, day: StemBranch, hour: StemBranch, note: String) {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        let comps = cal.dateComponents([.year, .month, .day, .hour, .minute], from: date)
        let y = comps.year ?? 2000
        let m = comps.month ?? 1
        let d = comps.day ?? 1
        let hour = comps.hour ?? 0

        let daySB = dayStemBranch(year: y, month: m, day: d, hour: hour, timeZone: timeZone)
        let hourSB = hourStemBranch(dayStem: daySB.stem, hour: hour)

        let (yearSB, yearNote) = yearStemBranch(for: date, timeZone: timeZone)
        let (monthSB, monthNote) = monthStemBranch(for: date, yearStem: yearSB.stem, timeZone: timeZone)
        let note = [yearNote, monthNote].joined(separator: "；")
        return (yearSB, monthSB, daySB, hourSB, note)
    }

    /// 指定参考时刻的流年干支（立春换年）。
    static func flowingYear(for date: Date, timeZone: TimeZone) -> StemBranch {
        yearStemBranch(for: date, timeZone: timeZone).0
    }

    /// 指定参考时刻的流月干支（节令换月）。
    static func flowingMonth(for date: Date, timeZone: TimeZone) -> StemBranch {
        let yearStem = yearStemBranch(for: date, timeZone: timeZone).0.stem
        return monthStemBranch(for: date, yearStem: yearStem, timeZone: timeZone).0
    }

    /// 指定参考时刻的流日干支。
    static func flowingDay(for date: Date, timeZone: TimeZone) -> StemBranch {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        let comps = cal.dateComponents([.year, .month, .day, .hour], from: date)
        return dayStemBranch(
            year: comps.year ?? 2000,
            month: comps.month ?? 1,
            day: comps.day ?? 1,
            hour: comps.hour ?? 12,
            timeZone: timeZone
        )
    }

    // MARK: - Year / Month via solar terms

    /// 节气索引：立春=2, 惊蛰=4, 清明=6, …（与 SolarTerms.names 一致）
    private static let jieIndicesForMonths: [Int] = [2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 0]
    // 寅月立春…丑月小寒；小寒 index=0 属上一年丑月尾段

    static func yearStemBranch(for date: Date, timeZone: TimeZone) -> (StemBranch, String) {
        _ = timeZone
        let lichun = nearestPastOrEqualTerm(named: "立春", onOrBefore: date)
            ?? SolarTerms.terms(forSolarYear: Calendar(identifier: .gregorian).component(.year, from: date)).first { $0.name == "立春" }!

        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(secondsFromGMT: 0)!
        // 八字年以立春后的「农历节气年」为准：取立春所在公历年作为干支年锚定
        // 若 date < 本年立春，则用上一年干支
        let yearOfLiChun = cal.component(.year, from: lichun.approximateDate)
        let ganzhiYear: Int
        if date >= lichun.approximateDate {
            ganzhiYear = yearOfLiChun
        } else {
            ganzhiYear = yearOfLiChun - 1
        }
        // 1984 = 甲子
        let sb = StemBranch.from(sexagenaryIndex: ganzhiYear - 1984)
        return (sb, "年柱以立春换年（本次立春参考 \(formatUTC(lichun.approximateDate))）")
    }

    static func monthStemBranch(for date: Date, yearStem: HeavenlyStem, timeZone: TimeZone) -> (StemBranch, String) {
        _ = timeZone
        let (jieMonth, jieName, jieDate) = jieMonthInfo(for: date)
        let startStem: Int
        switch yearStem {
        case .jia, .ji: startStem = HeavenlyStem.bing.rawValue
        case .yi, .geng: startStem = HeavenlyStem.wu.rawValue
        case .bing, .xin: startStem = HeavenlyStem.geng.rawValue
        case .ding, .ren: startStem = HeavenlyStem.ren.rawValue
        case .wu, .gui: startStem = HeavenlyStem.jia.rawValue
        }
        let stem = HeavenlyStem.from(index: startStem + (jieMonth - 1))
        let branch = EarthlyBranch.from(index: EarthlyBranch.yin.rawValue + (jieMonth - 1))
        let sb = StemBranch(stem: stem, branch: branch)
        return (sb, "月柱以\(jieName)换月（参考 \(formatUTC(jieDate))）")
    }

    /// 返回 1=寅月 … 12=丑月
    static func jieMonthInfo(for date: Date) -> (month: Int, name: String, date: Date) {
        let terms = SolarTerms.termsForNearbyYears(around: date)
        // 十二节：立春、惊蛰、清明、立夏、芒种、小暑、立秋、白露、寒露、立冬、大雪、小寒
        let jieNames = ["立春", "惊蛰", "清明", "立夏", "芒种", "小暑", "立秋", "白露", "寒露", "立冬", "大雪", "小寒"]
        let jies = terms.filter { jieNames.contains($0.name) }.sorted { $0.approximateDate < $1.approximateDate }
        guard let current = jies.last(where: { $0.approximateDate <= date }) ?? jies.first else {
            return (1, "立春", date)
        }
        let idx = jieNames.firstIndex(of: current.name) ?? 0
        // 小寒为丑月（12）
        let month = idx + 1
        return (month, current.name, current.approximateDate)
    }

    private static func nearestPastOrEqualTerm(named name: String, onOrBefore date: Date) -> SolarTermInfo? {
        let terms = SolarTerms.termsForNearbyYears(around: date).filter { $0.name == name }
        return terms.last(where: { $0.approximateDate <= date }) ?? terms.first
    }

    private static func formatUTC(_ date: Date) -> String {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.timeZone = TimeZone(secondsFromGMT: 8 * 3600) // 展示用东八区
        f.dateFormat = "yyyy-MM-dd HH:mm"
        return f.string(from: date) + " CST"
    }

    // MARK: - Day / Hour

    static func dayStemBranch(year: Int, month: Int, day: Int, hour: Int, timeZone: TimeZone) -> StemBranch {
        var y = year, m = month, d = day
        if hour >= 23 {
            var cal = Calendar(identifier: .gregorian)
            cal.timeZone = timeZone
            if let dt = cal.date(from: DateComponents(year: y, month: m, day: d)),
               let next = cal.date(byAdding: .day, value: 1, to: dt) {
                let c = cal.dateComponents([.year, .month, .day], from: next)
                y = c.year ?? y; m = c.month ?? m; d = c.day ?? d
            }
        }
        let jd = julianDay(year: y, month: m, day: d)
        // 1900-01-01 0h UT 的儒略日为 2415020.5；该日干支为甲戌（index 10）
        let baseJD = 2415020.5
        let baseIndex = 10 // 甲戌
        let delta = Int(floor(jd - baseJD))
        let idx = ((baseIndex + delta) % 60 + 60) % 60
        return StemBranch.from(sexagenaryIndex: idx)
    }

    static func hourStemBranch(dayStem: HeavenlyStem, hour: Int) -> StemBranch {
        let branch = EarthlyBranch.hourBranch(hour: hour)
        let startStem: Int
        switch dayStem {
        case .jia, .ji: startStem = HeavenlyStem.jia.rawValue
        case .yi, .geng: startStem = HeavenlyStem.bing.rawValue
        case .bing, .xin: startStem = HeavenlyStem.wu.rawValue
        case .ding, .ren: startStem = HeavenlyStem.geng.rawValue
        case .wu, .gui: startStem = HeavenlyStem.ren.rawValue
        }
        return StemBranch(stem: .from(index: startStem + branch.rawValue), branch: branch)
    }

    static func julianDay(year: Int, month: Int, day: Int) -> Double {
        var y = year, m = month
        if m <= 2 { y -= 1; m += 12 }
        let A = y / 100
        let B = 2 - A + A / 4
        return floor(365.25 * Double(y + 4716))
            + floor(30.6001 * Double(m + 1))
            + Double(day) + Double(B) - 1524.5
    }
}
