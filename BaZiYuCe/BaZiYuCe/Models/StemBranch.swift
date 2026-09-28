//
//  StemBranch.swift
//  BaZiYuCe
//

import Foundation

enum WuXing: Int, CaseIterable, Codable, Hashable, Identifiable {
    case wood = 0, fire, earth, metal, water

    var id: Int { rawValue }

    var name: String {
        ["木", "火", "土", "金", "水"][rawValue]
    }

    /// 我所生
    var generates: WuXing { WuXing(rawValue: (rawValue + 1) % 5)! }
    /// 我所克
    var controls: WuXing { WuXing(rawValue: (rawValue + 2) % 5)! }
    /// 生我者
    var generatedBy: WuXing { WuXing(rawValue: (rawValue + 4) % 5)! }
    /// 克我者
    var controlledBy: WuXing { WuXing(rawValue: (rawValue + 3) % 5)! }
}

enum YinYang: Int, Codable, Hashable {
    case yang = 0, yin

    var opposite: YinYang { self == .yang ? .yin : .yang }
    var name: String { self == .yang ? "阳" : "阴" }
}

enum HeavenlyStem: Int, CaseIterable, Codable, Hashable, Identifiable {
    case jia = 0, yi, bing, ding, wu, ji, geng, xin, ren, gui

    var id: Int { rawValue }

    var name: String {
        ["甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"][rawValue]
    }

    var yinYang: YinYang { rawValue % 2 == 0 ? .yang : .yin }

    var wuXing: WuXing {
        switch self {
        case .jia, .yi: return .wood
        case .bing, .ding: return .fire
        case .wu, .ji: return .earth
        case .geng, .xin: return .metal
        case .ren, .gui: return .water
        }
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

    var yinYang: YinYang { rawValue % 2 == 0 ? .yang : .yin }

    var wuXing: WuXing {
        switch self {
        case .yin, .mao: return .wood
        case .si, .wu: return .fire
        case .chen, .xu, .chou, .wei: return .earth
        case .shen, .you: return .metal
        case .hai, .zi: return .water
        }
    }

    /// 藏干（主气在前）
    var hiddenStems: [HeavenlyStem] {
        switch self {
        case .zi: return [.gui]
        case .chou: return [.ji, .gui, .xin]
        case .yin: return [.jia, .bing, .wu]
        case .mao: return [.yi]
        case .chen: return [.wu, .yi, .gui]
        case .si: return [.bing, .wu, .geng]
        case .wu: return [.ding, .ji]
        case .wei: return [.ji, .ding, .yi]
        case .shen: return [.geng, .ren, .wu]
        case .you: return [.xin]
        case .xu: return [.wu, .xin, .ding]
        case .hai: return [.ren, .jia]
        }
    }

    static func hourBranch(hour: Int) -> EarthlyBranch {
        let h = ((hour % 24) + 24) % 24
        let idx = ((h + 1) / 2) % 12
        return EarthlyBranch(rawValue: idx)!
    }

    static func from(index: Int) -> EarthlyBranch {
        EarthlyBranch(rawValue: ((index % 12) + 12) % 12)!
    }

    /// 六冲
    var clash: EarthlyBranch {
        EarthlyBranch.from(index: rawValue + 6)
    }

    /// 六合
    var sixHarmony: EarthlyBranch {
        let map = [0: 1, 1: 0, 2: 11, 3: 10, 4: 9, 5: 8, 6: 7, 7: 6, 8: 5, 9: 4, 10: 3, 11: 2]
        return EarthlyBranch(rawValue: map[rawValue]!)!
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
