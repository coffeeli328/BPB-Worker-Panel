//
//  TrueSolarTime.swift
//  QimenDunjia
//
//  钟表时 → 地方真太阳时（用于时辰/子时换日）。
//  SOLID: 经度改正 (λ−时区中央经线)×4 分钟 + NOAA/Meeus 均时差。
//  RESIDUAL: 未计大气折射/观测高度；均时差为低精度式，通常 <1–2 分钟。
//

import Foundation

struct LocationPreset: Identifiable, Hashable {
    let id: String
    let name: String
    let longitude: Double // 东经为正

    static let all: [LocationPreset] = [
        .init(id: "beijing", name: "北京", longitude: 116.4074),
        .init(id: "shanghai", name: "上海", longitude: 121.4737),
        .init(id: "guangzhou", name: "广州", longitude: 113.2644),
        .init(id: "chengdu", name: "成都", longitude: 104.0665),
        .init(id: "xian", name: "西安", longitude: 108.9398),
        .init(id: "wulumuqi", name: "乌鲁木齐", longitude: 87.6168),
        .init(id: "haerbin", name: "哈尔滨", longitude: 126.5340),
        .init(id: "hongkong", name: "香港", longitude: 114.1694),
        .init(id: "taipei", name: "台北", longitude: 121.5654),
        .init(id: "meridian120", name: "东经120°(东八区中央)", longitude: 120.0)
    ]
}

enum TrueSolarTime {

    /// 时区标准经线（度）：UTC 偏移小时 × 15。
    static func standardMeridianDegrees(timeZone: TimeZone, at date: Date) -> Double {
        Double(timeZone.secondsFromGMT(for: date)) / 3600.0 * 15.0
    }

    /// 经度改正（分钟）：东经大于中央经线则地方时更早（钟表偏慢，应加）。
    static func longitudeCorrectionMinutes(longitude: Double, timeZone: TimeZone, at date: Date) -> Double {
        let meridian = standardMeridianDegrees(timeZone: timeZone, at: date)
        return (longitude - meridian) * 4.0
    }

    /// 将民用钟表时刻转为真太阳时等价的绝对时刻（同一时区读出的时分即真太阳时）。
    /// 公式：真太阳时 ≈ 钟表时 + (λ−中央经线)×4min + 均时差。
    static func adjustedDate(
        civil: Date,
        timeZone: TimeZone,
        longitudeEastDegrees: Double,
        applyEquationOfTime: Bool = true
    ) -> (date: Date, longitudeMinutes: Double, eotMinutes: Double, totalMinutes: Double) {
        let lonMin = longitudeCorrectionMinutes(longitude: longitudeEastDegrees, timeZone: timeZone, at: civil)
        var eot: Double = 0
        if applyEquationOfTime {
            let jd = AstronomyCore.julianDay(from: civil)
            // 均时差用 UT≈TT 足够（差 ΔT 对 EoT 影响可忽略）
            let jde = jd + AstronomyCore.deltaTSeconds(year: yearUT(of: civil)) / 86400.0
            eot = AstronomyCore.equationOfTimeMinutes(jde: jde)
        }
        let total = lonMin + eot
        let adjusted = civil.addingTimeInterval(total * 60.0)
        return (adjusted, lonMin, eot, total)
    }

    private static func yearUT(of date: Date) -> Double {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(secondsFromGMT: 0)!
        return Double(cal.component(.year, from: date))
    }
}
