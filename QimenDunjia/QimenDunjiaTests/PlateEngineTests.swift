//
//  PlateEngineTests.swift
//  QimenDunjiaTests
//
//  黄金用例：锁定转盘+拆补核心排盘（不依赖历法/节气）。
//  在 Mac 上用 Xcode Test 运行；Linux 可用 verify_golden_cases.py 对照。
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
        XCTAssertEqual(plate.xunKongBranches.map(\.name), ["戌", "亥"])

        XCTAssertEqual(plate.stars[.kan1], .peng)
        XCTAssertEqual(plate.stars[.gen8], .ren)
        XCTAssertEqual(plate.gates[.kan1], .xiu)
        XCTAssertEqual(plate.gates[.gen8], .sheng)
        XCTAssertEqual(plate.deities[.kan1], .zhiFu)
        XCTAssertEqual(plate.deities[.gen8], .tengShe)
        XCTAssertEqual(plate.deities[.qian6], .jiuTian)
    }

    func testYangDun1DingMao() {
        let hour = StemBranch.parse("丁卯")!
        let plate = QimenEngine.buildPlate(isYangDun: true, juNumber: 1, hour: hour)
        XCTAssertEqual(plate.zhiFuStar, .peng)
        XCTAssertEqual(plate.zhiFuPalace, .dui7)
        XCTAssertEqual(plate.zhiShiGate, .xiu)
        XCTAssertEqual(plate.zhiShiPalace, .xun4)
        XCTAssertEqual(plate.stars[.dui7], .peng)
        XCTAssertEqual(plate.gates[.xun4], .xiu)
    }

    func testYangDun6YiSi() {
        let hour = StemBranch.parse("乙巳")!
        let plate = QimenEngine.buildPlate(isYangDun: true, juNumber: 6, hour: hour)
        XCTAssertEqual(stemMap(plate.earth), [
            6: "戊", 7: "己", 8: "庚", 9: "辛", 1: "壬", 2: "癸", 3: "丁", 4: "丙", 5: "乙"
        ])
        XCTAssertEqual(plate.yiStem, .ren)
        XCTAssertEqual(plate.xunShouPalace, .kan1)
        XCTAssertEqual(plate.zhiFuStar, .peng)
        XCTAssertEqual(plate.zhiShiGate, .xiu)
        XCTAssertEqual(plate.zhiFuPalace, .kun2) // 乙在中寄坤
        XCTAssertEqual(plate.zhiShiPalace, .kun2)
        XCTAssertEqual(plate.deities[.kun2], .zhiFu)
        XCTAssertEqual(plate.deities[.dui7], .tengShe)
        XCTAssertEqual(plate.deities[.li9], .jiuTian)
    }

    func testYinDun9BingYin() {
        let hour = StemBranch.parse("丙寅")!
        let plate = QimenEngine.buildPlate(isYangDun: false, juNumber: 9, hour: hour)
        XCTAssertEqual(plate.earth[.li9], .wu)
        XCTAssertEqual(plate.earth[.kun2], .bing)
        XCTAssertEqual(plate.zhiFuStar, .ying)
        XCTAssertEqual(plate.zhiShiGate, .jing)
        XCTAssertEqual(plate.zhiFuPalace, .kun2)
        XCTAssertEqual(plate.zhiShiPalace, .dui7)
    }

    func testJuResolverTable() {
        XCTAssertEqual(JuResolver.ju(term: "冬至", yuanIndex: 0)?.ju, 1)
        XCTAssertEqual(JuResolver.ju(term: "芒种", yuanIndex: 0)?.ju, 6)
        XCTAssertEqual(JuResolver.ju(term: "夏至", yuanIndex: 0)?.ju, 9)
        XCTAssertEqual(JuResolver.ju(term: "立夏", yuanIndex: 2)?.ju, 7)
    }

    func testFuTouYuan() {
        let wuWu = StemBranch.parse("壬午")!
        let ft = JuResolver.fuTou(for: wuWu)
        XCTAssertEqual(ft.name, "己卯")
        XCTAssertEqual(JuResolver.yuanIndex(fuTou: ft), 0)

        let renXu = StemBranch.parse("壬戌")!
        let ft2 = JuResolver.fuTou(for: renXu)
        XCTAssertEqual(ft2.name, "己未")
        XCTAssertEqual(JuResolver.yuanIndex(fuTou: ft2), 2)
    }

    private func stemMap(_ earth: [Palace: HeavenlyStem]) -> [Int: String] {
        Dictionary(uniqueKeysWithValues: earth.map { ($0.key.rawValue, $0.value.name) })
    }
}
