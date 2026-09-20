//
//  PlateEngineTests.swift
//  QimenDunjiaTests
//

import XCTest
@testable import QimenDunjia

final class PlateEngineTests: XCTestCase {

    func testYangDun1JiaZiFuYin() {
        let hour = StemBranch.parse("甲子")!
        let plate = QimenEngine.buildPlate(isYangDun: true, juNumber: 1, hour: hour)
        XCTAssertEqual(stemMap(plate.earth), [
            1: "戊", 2: "己", 3: "庚", 4: "辛", 5: "壬", 6: "癸", 7: "丁", 8: "丙", 9: "乙"
        ])
        XCTAssertEqual(plate.zhiFuStar, .peng)
        XCTAssertEqual(plate.zhiShiGate, .xiu)
        XCTAssertEqual(plate.zhiFuPalace, .kan1)
        XCTAssertEqual(plate.zhiShiPalace, .kan1)
    }

    func testYangDun1DingMao() {
        let hour = StemBranch.parse("丁卯")!
        let plate = QimenEngine.buildPlate(isYangDun: true, juNumber: 1, hour: hour)
        XCTAssertEqual(plate.zhiFuPalace, .dui7)
        XCTAssertEqual(plate.zhiShiPalace, .xun4)
    }

    func testYangDun6YiSi() {
        let hour = StemBranch.parse("乙巳")!
        let plate = QimenEngine.buildPlate(isYangDun: true, juNumber: 6, hour: hour)
        XCTAssertEqual(plate.zhiFuPalace, .kun2)
        XCTAssertEqual(plate.zhiShiPalace, .kun2)
    }

    func testYinDun9BingYin() {
        let hour = StemBranch.parse("丙寅")!
        let plate = QimenEngine.buildPlate(isYangDun: false, juNumber: 9, hour: hour)
        XCTAssertEqual(plate.zhiFuStar, .ying)
        XCTAssertEqual(plate.zhiShiPalace, .dui7)
    }

    func testJuResolverAndFuTou() {
        XCTAssertEqual(JuResolver.ju(term: "冬至", yuanIndex: 0)?.ju, 1)
        XCTAssertEqual(JuResolver.fuTou(for: StemBranch.parse("壬午")!).name, "己卯")
    }

    // MARK: - Astronomy / solar terms

    func testSolarTermLichun2024Window() {
        let instant = SolarTerms.instant(year: 2024, termIndex: 2) // 立春
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(secondsFromGMT: 0)!
        let c = cal.dateComponents([.year, .month, .day, .hour], from: instant)
        XCTAssertEqual(c.year, 2024)
        XCTAssertEqual(c.month, 2)
        XCTAssertEqual(c.day, 4)
        XCTAssertEqual(c.hour, 8) // ~08:20 UTC
    }

    func testSolarTermBoundaryFlip() {
        let lichun = SolarTerms.instant(year: 2024, termIndex: 2)
        let before = lichun.addingTimeInterval(-5 * 60)
        let after = lichun.addingTimeInterval(5 * 60)
        let tz = TimeZone(secondsFromGMT: 8 * 3600)!
        XCTAssertEqual(SolarTerms.currentTerm(for: before, timeZone: tz).name, "大寒")
        XCTAssertEqual(SolarTerms.currentTerm(for: after, timeZone: tz).name, "立春")
    }

    func testEquationOfTimeMeeusExample() {
        let jd = AstronomyCore.julianDay(year: 1992, month: 10, day: 13, hourUT: 12)
        let eot = AstronomyCore.equationOfTimeMinutes(jde: jd)
        XCTAssertTrue((12.5...15.0).contains(eot), "EoT was \(eot)")
    }

    func testLongitudeCorrectionBeijing() {
        let tz = TimeZone(secondsFromGMT: 8 * 3600)!
        let date = Date(timeIntervalSince1970: 1_700_000_000)
        let minutes = TrueSolarTime.longitudeCorrectionMinutes(longitude: 116.4074, timeZone: tz, at: date)
        XCTAssertTrue((-15.0 ... -13.5).contains(minutes), "got \(minutes)")
    }

    func testUrumqiVersusMeridian120HourBranch() {
        let tz = TimeZone(secondsFromGMT: 8 * 3600)!
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = tz
        // 2024-06-15 00:50 CST
        let civil = cal.date(from: DateComponents(year: 2024, month: 6, day: 15, hour: 0, minute: 50))!

        let at120 = TrueSolarTime.adjustedDate(
            civil: civil, timeZone: tz, longitudeEastDegrees: 120.0, applyEquationOfTime: false
        )
        let atUrumqi = TrueSolarTime.adjustedDate(
            civil: civil, timeZone: tz, longitudeEastDegrees: 87.6168, applyEquationOfTime: false
        )

        let h120 = cal.component(.hour, from: at120.date)
        let m120 = cal.component(.minute, from: at120.date)
        let hU = cal.component(.hour, from: atUrumqi.date)
        let mU = cal.component(.minute, from: atUrumqi.date)

        XCTAssertEqual(EarthlyBranch.hourBranch(hour: h120).name, "子")
        // ~22:40 → 亥
        XCTAssertEqual(EarthlyBranch.hourBranch(hour: hU).name, "亥")
        XCTAssertNotEqual(h120 * 60 + m120, hU * 60 + mU)
    }

    func testGenerateUsesTrueSolarFlag() {
        let tzSec = 8 * 3600
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(secondsFromGMT: tzSec)!
        let civil = cal.date(from: DateComponents(year: 2024, month: 6, day: 15, hour: 0, minute: 50))!

        var reqOn = ChartRequest(date: civil, timeZoneSecondsFromGMT: tzSec, longitude: 87.6168, useTrueSolarTime: true)
        reqOn.locationNote = "乌鲁木齐"
        var reqOff = reqOn
        reqOff.useTrueSolarTime = false

        let on = QimenEngine.generate(request: reqOn)
        let off = QimenEngine.generate(request: reqOff)
        XCTAssertTrue(on.usedTrueSolarTime)
        XCTAssertFalse(off.usedTrueSolarTime)
        XCTAssertNotEqual(on.hourSB.branch, off.hourSB.branch)
    }

    // MARK: - 置闰

    func testZhiYunIntercalationMangZhong2023() {
        let tz = TimeZone(secondsFromGMT: 8 * 3600)!
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = tz
        let day = cal.date(from: DateComponents(year: 2023, month: 6, day: 8, hour: 12))!
        let sb = GanzhiCalendar.stemBranchFourPillars(for: day, timeZone: tz).day
        let ju = ZhiYunResolver.resolve(queryDate: day, day: sb, timeZone: tz)
        XCTAssertEqual(ju.juMethod, .zhiYun)
        XCTAssertTrue(ju.isRunQi)
        XCTAssertTrue(ju.solarTermName.contains("芒种"))
        XCTAssertEqual(ju.yuanIndex, 0)
        XCTAssertEqual(ju.juNumber, 6)
        XCTAssertTrue(ju.isYangDun)
        XCTAssertEqual(ju.phase, .runQi)
    }

    func testZhiYunDivergesFromChaibuOnChaoShen() {
        let tz = TimeZone(secondsFromGMT: 8 * 3600)!
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = tz
        let day = cal.date(from: DateComponents(year: 2023, month: 5, day: 1, hour: 12))!
        let sb = GanzhiCalendar.stemBranchFourPillars(for: day, timeZone: tz).day
        let chai = JuResolver.resolve(day: sb, queryDate: day, timeZone: tz, method: .chaiBu)
        let zhi = JuResolver.resolve(day: sb, queryDate: day, timeZone: tz, method: .zhiYun)
        XCTAssertEqual(chai.solarTermName, "谷雨")
        XCTAssertTrue(zhi.solarTermName.contains("立夏"))
        XCTAssertNotEqual(chai.juNumber, zhi.juNumber)
    }

    func testDefaultJuMethodIsChaibu() {
        XCTAssertEqual(ChartRequest().juMethod, .chaiBu)
        let tz = TimeZone(secondsFromGMT: 8 * 3600)!
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = tz
        let day = cal.date(from: DateComponents(year: 2024, month: 3, day: 25, hour: 12))!
        var req = ChartRequest(date: day, timeZoneSecondsFromGMT: 8 * 3600, useTrueSolarTime: false)
        let chart = QimenEngine.generate(request: req)
        XCTAssertEqual(chart.juMethod, .chaiBu)
        XCTAssertEqual(chart.juNumber, 9) // 春分中元常见
    }

    private func stemMap(_ earth: [Palace: HeavenlyStem]) -> [Int: String] {
        Dictionary(uniqueKeysWithValues: earth.map { ($0.key.rawValue, $0.value.name) })
    }
}
