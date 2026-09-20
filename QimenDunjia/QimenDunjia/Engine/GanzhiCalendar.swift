//
//  GanzhiCalendar.swift
//  QimenDunjia
//
//  四柱干支。日柱用儒略日推算；时柱五鼠遁。
//  APPROXIMATE: 年柱立春取 2/4；月柱节令取固定月日；未做真太阳时校正。
//  SOLID: 时柱五鼠遁；日柱子时（23点）换日。
//

import Foundation

enum GanzhiCalendar {

    static func stemBranchFourPillars(for date: Date, timeZone: TimeZone) -> (year: StemBranch, month: StemBranch, day: StemBranch, hour: StemBranch) {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        let comps = cal.dateComponents([.year, .month, .day, .hour, .minute], from: date)
        let y = comps.year ?? 2000
        let m = comps.month ?? 1
        let d = comps.day ?? 1
        let hour = comps.hour ?? 0

        let daySB = dayStemBranch(year: y, month: m, day: d, hour: hour, timeZone: timeZone)
        let hourSB = hourStemBranch(dayStem: daySB.stem, hour: hour)
        let yearSB = yearStemBranch(solarYear: y, month: m, day: d)
        let monthSB = monthStemBranch(yearStem: yearSB.stem, month: m, day: d)
        return (yearSB, monthSB, daySB, hourSB)
    }

    static func yearStemBranch(solarYear: Int, month: Int, day: Int) -> StemBranch {
        var y = solarYear
        if month < 2 || (month == 2 && day < 4) { y -= 1 }
        return StemBranch.from(sexagenaryIndex: y - 1984)
    }

    static func monthStemBranch(yearStem: HeavenlyStem, month: Int, day: Int) -> StemBranch {
        let jieMonth = approximateJieMonth(month: month, day: day)
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
        return StemBranch(stem: stem, branch: branch)
    }

    private static func approximateJieMonth(month: Int, day: Int) -> Int {
        let boundaries: [(Int, Int)] = [
            (2, 4), (3, 6), (4, 5), (5, 6), (6, 6), (7, 7),
            (8, 8), (9, 8), (10, 8), (11, 7), (12, 7), (1, 6)
        ]
        let md = month * 100 + day
        if md < 106 { return 12 }
        for i in 0..<11 {
            let a = boundaries[i].0 * 100 + boundaries[i].1
            let b = boundaries[i + 1].0 * 100 + boundaries[i + 1].1
            if md >= a && md < b { return i + 1 }
        }
        if md >= 1207 { return 11 }
        return 12
    }

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
        let baseJD = 2415021.0 // 1900-01-01
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

    static func lunarDescription(for date: Date, timeZone: TimeZone) -> String {
        var chinese = Calendar(identifier: .chinese)
        chinese.timeZone = timeZone
        let c = chinese.dateComponents([.year, .month, .day], from: date)
        let stems = ["甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"]
        let branches = ["子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"]
        let yIdx = ((c.year ?? 1) - 1) % 60
        let yearName = stems[yIdx % 10] + branches[yIdx % 12]
        let months = ["正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"]
        let m = max(1, min(12, c.month ?? 1))
        return "\(yearName)年\(months[m - 1])月\(c.day ?? 1)日"
    }
}
