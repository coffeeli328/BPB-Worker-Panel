//
//  Palace.swift
//  QimenDunjia
//
//  九宫、九星、八门、八神 — 转盘法先天落位与顺时针环序。
//

import Foundation

enum Palace: Int, CaseIterable, Codable, Hashable, Identifiable {
    case kan1 = 1, kun2 = 2, zhen3 = 3, xun4 = 4, zhong5 = 5
    case qian6 = 6, dui7 = 7, gen8 = 8, li9 = 9

    var id: Int { rawValue }

    var name: String {
        ["", "坎", "坤", "震", "巽", "中", "乾", "兑", "艮", "离"][rawValue]
    }

    var direction: String {
        switch self {
        case .kan1: return "北"
        case .kun2: return "西南"
        case .zhen3: return "东"
        case .xun4: return "东南"
        case .zhong5: return "中"
        case .qian6: return "西北"
        case .dui7: return "西"
        case .gen8: return "东北"
        case .li9: return "南"
        }
    }

    /// UI 九宫（离南在上）
    static var gridOrder: [[Palace]] {
        [
            [.xun4, .li9, .kun2],
            [.zhen3, .zhong5, .dui7],
            [.gen8, .kan1, .qian6]
        ]
    }

    /// 飞布地盘用：阳顺 1…9
    static let yangFly: [Palace] = [.kan1, .kun2, .zhen3, .xun4, .zhong5, .qian6, .dui7, .gen8, .li9]
    /// 阴逆 9…1
    static let yinFly: [Palace] = [.li9, .gen8, .dui7, .qian6, .zhong5, .xun4, .zhen3, .kun2, .kan1]

    /// 转盘顺时针八宫环（不含中）：坎→艮→震→巽→离→坤→兑→乾
    /// SOLID: 转盘法九星/八门按此环顺时针排布（不论阴阳遁）。
    static let clockRing: [Palace] = [.kan1, .gen8, .zhen3, .xun4, .li9, .kun2, .dui7, .qian6]

    /// 中五寄宫：时家常用阳遁寄艮八、阴遁寄坤二；排盘「落中」展示多寄坤二。
    /// AMBIGUITY: 寄宫流派不一；本实现：时干/值符落中一律寄坤二（与多数现代排盘软件一致）。
    static let zhongHost: Palace = .kun2

    func resolvingZhong() -> Palace {
        self == .zhong5 ? Self.zhongHost : self
    }
}

/// 九星（洛书先天宫）
enum NineStar: Int, CaseIterable, Codable, Hashable, Identifiable {
    case peng = 1, rui = 2, chong = 3, fu = 4, qin = 5
    case xin = 6, zhu = 7, ren = 8, ying = 9

    var id: Int { rawValue }
    var innatePalace: Palace { Palace(rawValue: rawValue)! }

    var name: String {
        ["", "天蓬", "天芮", "天冲", "天辅", "天禽", "天心", "天柱", "天任", "天英"][rawValue]
    }

    var shortName: String {
        ["", "蓬", "芮", "冲", "辅", "禽", "心", "柱", "任", "英"][rawValue]
    }

    var isAuspicious: Bool {
        self == .chong || self == .fu || self == .xin || self == .ren
    }

    /// 转盘环上的八星序（禽随芮，不单独占环位）
    static let clockStars: [NineStar] = [.peng, .ren, .chong, .fu, .ying, .rui, .zhu, .xin]

    static func from(palace: Palace) -> NineStar {
        NineStar(rawValue: palace.rawValue)!
    }
}

/// 八门（先天：休坎、死坤、伤震、杜巽、中无门、开乾、惊兑、生艮、景离）
enum EightGate: String, CaseIterable, Codable, Hashable, Identifiable {
    case xiu, si, shang, du, jing, kai, jingShock, sheng

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .xiu: return "休门"
        case .si: return "死门"
        case .shang: return "伤门"
        case .du: return "杜门"
        case .jing: return "景门"
        case .kai: return "开门"
        case .jingShock: return "惊门"
        case .sheng: return "生门"
        }
    }

    var isAuspicious: Bool {
        self == .xiu || self == .sheng || self == .kai || self == .jing
    }

    /// 先天落宫
    var innatePalace: Palace {
        switch self {
        case .xiu: return .kan1
        case .si: return .kun2
        case .shang: return .zhen3
        case .du: return .xun4
        case .jing: return .li9
        case .kai: return .qian6
        case .jingShock: return .dui7
        case .sheng: return .gen8
        }
    }

    /// 转盘顺时针八门序：休生伤杜景死惊开
    static let clockGates: [EightGate] = [.xiu, .sheng, .shang, .du, .jing, .si, .jingShock, .kai]

    static func innate(at palace: Palace) -> EightGate? {
        clockGates.first { $0.innatePalace == palace }
            ?? EightGate.allCases.first { $0.innatePalace == palace }
    }
}

/// 八神
enum EightDeity: Int, CaseIterable, Codable, Hashable, Identifiable {
    case zhiFu, tengShe, taiYin, liuHe, baiHu, xuanWu, jiuDi, jiuTian

    var id: Int { rawValue }

    func name(isYangDun: Bool) -> String {
        switch self {
        case .zhiFu: return "值符"
        case .tengShe: return "腾蛇"
        case .taiYin: return "太阴"
        case .liuHe: return "六合"
        case .baiHu: return isYangDun ? "白虎" : "勾陈"
        case .xuanWu: return isYangDun ? "玄武" : "朱雀"
        case .jiuDi: return "九地"
        case .jiuTian: return "九天"
        }
    }

    var shortName: String {
        ["符", "蛇", "阴", "合", "虎", "武", "地", "天"][rawValue]
    }
}
