//
//  AstronomyCore.swift
//  QimenDunjia
//
//  Meeus 低精度太阳黄经 + ΔT（1900–2100 可用）。
//  口径：Jean Meeus, Astronomical Algorithms (2nd ed.) Ch.25 简化式。
//  RESIDUAL: 1900–2100 节气时刻相对精密星历通常约数分钟量级；非 VSOP87/DE 级。
//

import Foundation

enum AstronomyCore {

    static let j2000 = 2451545.0

    /// Espenak / 常用多项式近似 ΔT = TT−UT（秒）。1900–2100。
    static func deltaTSeconds(year: Double) -> Double {
        let y = year
        let t = y - 2000.0
        if y < 1900 || y > 2100 {
            // 区间外粗估
            let u = (y - 1820.0) / 100.0
            return -20.0 + 32.0 * u * u
        }
        if y < 1920 {
            return -2.79 + 1.494119 * t - 0.0598939 * t * t + 0.0061966 * t * t * t - 0.000197 * t * t * t * t
        }
        if y < 1941 {
            return 21.20 + 0.84493 * (y - 1920) - 0.07613 * pow(y - 1920, 2) + 0.0020936 * pow(y - 1920, 3)
        }
        if y < 1961 {
            let u = y - 1950
            return 29.07 + 0.407 * u - pow(u, 2) / 233.0 + pow(u, 3) / 2547.0
        }
        if y < 1986 {
            let u = y - 1975
            return 45.45 + 1.067 * u - pow(u, 2) / 260.0 - pow(u, 3) / 718.0
        }
        if y < 2005 {
            let u = y - 2000
            return 63.86 + 0.3345 * u - 0.060374 * u * u + 0.0017275 * u * u * u
                + 0.000651814 * pow(u, 4) + 0.00002373599 * pow(u, 5)
        }
        if y < 2050 {
            return 62.92 + 0.32217 * t + 0.005589 * t * t
        }
        // 2050–2100
        return -20 + 32 * pow((y - 1820) / 100, 2) - 0.5628 * (2100 - y)
    }

    /// 视黄经（度，0…360），输入力学时 JDE。
    static func sunApparentLongitudeDegrees(jde: Double) -> Double {
        let T = (jde - j2000) / 36525.0
        let L0 = rev360(280.46646 + 36000.76983 * T + 0.0003032 * T * T)
        let M = rev360(357.52911 + 35999.05029 * T - 0.0001537 * T * T)
        let Mr = M * .pi / 180
        let C = (1.914602 - 0.004817 * T - 0.000014 * T * T) * sin(Mr)
            + (0.019993 - 0.000101 * T) * sin(2 * Mr)
            + 0.000289 * sin(3 * Mr)
        let trueLong = L0 + C
        let omega = 125.04 - 1934.136 * T
        return rev360(trueLong - 0.00569 - 0.00478 * sin(omega * .pi / 180))
    }

    /// NOAA / Meeus 均时差（分钟）：真太阳时 − 平太阳时。
    static func equationOfTimeMinutes(jde: Double) -> Double {
        let T = (jde - j2000) / 36525.0
        let L0 = rev360(280.46646 + 36000.76983 * T + 0.0003032 * T * T)
        let M = 357.52911 + 35999.05029 * T - 0.0001537 * T * T
        let e = 0.016708634 - T * (0.000042037 + 0.0000001267 * T)
        let eps0 = 23.0 + (26.0 + (21.448 - T * (46.815 + T * (0.00059 - T * 0.001813))) / 60.0) / 60.0
        let omega = 125.04 - 1934.136 * T
        let eps = eps0 + 0.00256 * cos(omega * .pi / 180)
        let y = pow(tan(eps / 2 * .pi / 180), 2)
        let Mr = M * .pi / 180
        let L0r = L0 * .pi / 180
        let eotRad = y * sin(2 * L0r)
            - 2 * e * sin(Mr)
            + 4 * e * y * sin(Mr) * cos(2 * L0r)
            - 0.5 * y * y * sin(4 * L0r)
            - 1.25 * e * e * sin(2 * Mr)
        return 4.0 * eotRad * 180 / .pi
    }

    /// 牛顿法求视黄经 = target 的 JDE（TT）。
    static func solveSolarLongitude(targetDegrees: Double, jdeGuess: Double) -> Double {
        var jde = jdeGuess
        let target = rev360(targetDegrees)
        for _ in 0..<16 {
            let lon = sunApparentLongitudeDegrees(jde: jde)
            var diff = lon - target
            while diff > 180 { diff -= 360 }
            while diff < -180 { diff += 360 }
            let h = 0.02
            var d1 = sunApparentLongitudeDegrees(jde: jde + h) - sunApparentLongitudeDegrees(jde: jde - h)
            while d1 > 180 { d1 -= 360 }
            while d1 < -180 { d1 += 360 }
            let rate = d1 / (2 * h)
            guard abs(rate) > 1e-12 else { break }
            let corr = diff / rate
            jde -= corr
            if abs(corr) < 1e-8 { break }
        }
        return jde
    }

    // MARK: - Julian Day

    /// 公历 UT → JD
    static func julianDay(year: Int, month: Int, day: Int, hourUT: Double) -> Double {
        var y = year, m = month
        if m <= 2 {
            y -= 1
            m += 12
        }
        let A = y / 100
        let B = 2 - A + A / 4
        let dayFrac = Double(day) + hourUT / 24.0
        return floor(365.25 * Double(y + 4716))
            + floor(30.6001 * Double(m + 1))
            + dayFrac + Double(B) - 1524.5
    }

    static func julianDay(from date: Date) -> Double {
        // UT components
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(secondsFromGMT: 0)!
        let c = cal.dateComponents([.year, .month, .day, .hour, .minute, .second], from: date)
        let hour = Double(c.hour ?? 0) + Double(c.minute ?? 0) / 60.0 + Double(c.second ?? 0) / 3600.0
        return julianDay(year: c.year ?? 2000, month: c.month ?? 1, day: c.day ?? 1, hourUT: hour)
    }

    /// JD(UT) → Date
    static func date(fromJulianDayUT jd: Double) -> Date {
        let Z = Int(floor(jd + 0.5))
        let F = jd + 0.5 - Double(Z)
        let A: Int
        if Z < 2299161 {
            A = Z
        } else {
            let alpha = Int(floor((Double(Z) - 1867216.25) / 36524.25))
            A = Z + 1 + alpha - alpha / 4
        }
        let B = A + 1524
        let C = Int(floor((Double(B) - 122.1) / 365.25))
        let D = Int(floor(365.25 * Double(C)))
        let E = Int(floor((Double(B - D)) / 30.6001))
        let day = Double(B - D - Int(floor(30.6001 * Double(E)))) + F
        let month = E < 14 ? E - 1 : E - 13
        let year = month > 2 ? C - 4716 : C - 4715
        let dayI = Int(floor(day))
        let frac = day - Double(dayI)
        let hours = frac * 24
        let h = Int(floor(hours))
        let minutes = (hours - Double(h)) * 60
        let mi = Int(floor(minutes))
        let sec = (minutes - Double(mi)) * 60
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(secondsFromGMT: 0)!
        var comps = DateComponents()
        comps.year = year
        comps.month = month
        comps.day = dayI
        comps.hour = h
        comps.minute = mi
        comps.second = Int(floor(sec))
        comps.nanosecond = Int((sec - floor(sec)) * 1e9)
        return cal.date(from: comps) ?? Date(timeIntervalSince1970: (jd - 2440587.5) * 86400)
    }

    static func rev360(_ x: Double) -> Double {
        let r = x.truncatingRemainder(dividingBy: 360)
        return r >= 0 ? r : r + 360
    }
}
