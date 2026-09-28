//
//  BaZiEngineTests.swift
//  BaZiYuCeTests
//

import XCTest
@testable import BaZiYuCe

final class BaZiEngineTests: XCTestCase {

    private var tz: TimeZone { TimeZone(identifier: "Asia/Shanghai")! }

    func testDayPillarKnownAnchors() {
        // 1900-01-01 → 甲戌（引擎基准）
        let d1900 = GanzhiCalendar.dayStemBranch(year: 1900, month: 1, day: 1, hour: 12, timeZone: tz)
        XCTAssertEqual(d1900.name, "甲戌")

        // 2000-01-01 中午 → 戊午（与儒略日推算一致）
        let d2000 = GanzhiCalendar.dayStemBranch(year: 2000, month: 1, day: 1, hour: 12, timeZone: tz)
        XCTAssertEqual(d2000.name, "戊午")
    }

    func testZiHourRollsDay() {
        let before = GanzhiCalendar.dayStemBranch(year: 2000, month: 1, day: 1, hour: 22, timeZone: tz)
        let after = GanzhiCalendar.dayStemBranch(year: 2000, month: 1, day: 1, hour: 23, timeZone: tz)
        XCTAssertEqual(before.name, "戊午")
        XCTAssertEqual(after.name, "己未")
    }

    func testHourStemWuShuDun() {
        // 甲日子时起甲
        let h = GanzhiCalendar.hourStemBranch(dayStem: .jia, hour: 0)
        XCTAssertEqual(h.name, "甲子")
        let h2 = GanzhiCalendar.hourStemBranch(dayStem: .jia, hour: 10)
        XCTAssertEqual(h2.branch, .si)
        XCTAssertEqual(h2.stem, .ji)
    }

    func testTenGodsRelativeToDayMaster() {
        // 甲日主见乙 = 劫财；见丙 = 食神；见戊 = 偏财；见庚 = 七杀；见壬 = 偏印
        XCTAssertEqual(TenGods.of(stem: .yi, dayMaster: .jia), .jieCai)
        XCTAssertEqual(TenGods.of(stem: .bing, dayMaster: .jia), .shiShen)
        XCTAssertEqual(TenGods.of(stem: .wu, dayMaster: .jia), .pianCai)
        XCTAssertEqual(TenGods.of(stem: .geng, dayMaster: .jia), .qiSha)
        XCTAssertEqual(TenGods.of(stem: .ren, dayMaster: .jia), .pianYin)
        XCTAssertEqual(TenGods.of(stem: .jia, dayMaster: .jia), .biJian)
    }

    func testChartAndForecastWireUp() {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = tz
        let birth = cal.date(from: DateComponents(year: 1990, month: 5, day: 15, hour: 10, minute: 0))!
        let chart = BaZiEngine.chart(birthDate: birth, gender: .male, timeZone: tz)
        XCTAssertEqual(chart.pillars.count, 4)
        XCTAssertFalse(chart.fourPillarsText.isEmpty)

        let forecasts = PredictionEngine.allForecasts(chart: chart, at: birth, timeZone: tz)
        XCTAssertEqual(forecasts.count, 3)
        for f in forecasts {
            XCTAssertFalse(f.summary.isEmpty)
            XCTAssertFalse(f.tips.isEmpty)
            XCTAssertGreaterThanOrEqual(f.score, 25)
            XCTAssertLessThanOrEqual(f.score, 95)
        }
    }

    func testBranchClash() {
        let notes = BranchRelations.describe(flowing: .zi, natal: [.wu, .mao])
        XCTAssertTrue(notes.contains(where: { $0.contains("冲") }))
    }
}
