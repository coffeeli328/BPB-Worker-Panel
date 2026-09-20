//
//  StemBranch.swift
//  QimenDunjia
//

import Foundation

enum HeavenlyStem: Int, CaseIterable, Codable, Hashable, Identifiable {
    case jia = 0, yi, bing, ding, wu, ji, geng, xin, ren, gui

    var id: Int { rawValue }

    var name: String {
        ["甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"][rawValue]
    }

    static func from(index: Int) -> HeavenlyStem {
        HeavenlyStem(rawValue: ((index % 10) + 10) % 10)!
    }
}

enum EarthlyBranch: Int, CaseIterable, Codable, Hashable, Identifiable {
    case zi = 0, chou, yin, mao, chen, si, wu, wei, shen, you, xu, hai

    var id: Int { rawValue }

    var name: String {
        ["子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"][rawValue]
    }

    static func hourBranch(hour: Int) -> EarthlyBranch {
        let h = ((hour % 24) + 24) % 24
        let idx = ((h + 1) / 2) % 12
        return EarthlyBranch(rawValue: idx)!
    }

    static func from(index: Int) -> EarthlyBranch {
        EarthlyBranch(rawValue: ((index % 12) + 12) % 12)!
    }
}

struct StemBranch: Codable, Hashable, Identifiable {
    let stem: HeavenlyStem
    let branch: EarthlyBranch

    var id: String { name }
    var name: String { stem.name + branch.name }

    var sexagenaryIndex: Int {
        for i in 0..<60 {
            if HeavenlyStem.from(index: i) == stem && EarthlyBranch.from(index: i) == branch {
                return i
            }
        }
        return 0
    }

    static func from(sexagenaryIndex i: Int) -> StemBranch {
        let n = ((i % 60) + 60) % 60
        return StemBranch(stem: .from(index: n), branch: .from(index: n))
    }

    static func parse(_ text: String) -> StemBranch? {
        guard text.count == 2,
              let sIdx = ["甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"].firstIndex(of: String(text.prefix(1))),
              let bIdx = ["子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"].firstIndex(of: String(text.suffix(1)))
        else { return nil }
        return StemBranch(stem: HeavenlyStem(rawValue: sIdx)!, branch: EarthlyBranch(rawValue: bIdx)!)
    }
}
