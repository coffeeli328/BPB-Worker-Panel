//
//  BaZiModels.swift
//  BaZiYuCe
//

import Foundation

enum Gender: String, CaseIterable, Codable, Identifiable {
    case male
    case female

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .male: return "男"
        case .female: return "女"
        }
    }
}

enum PillarKind: String, CaseIterable, Identifiable {
    case year, month, day, hour

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .year: return "年柱"
        case .month: return "月柱"
        case .day: return "日柱"
        case .hour: return "时柱"
        }
    }
}

enum TenGod: String, CaseIterable, Codable, Hashable, Identifiable {
    case biJian      // 比肩
    case jieCai      // 劫财
    case shiShen     // 食神
    case shangGuan   // 伤官
    case pianCai     // 偏财
    case zhengCai    // 正财
    case qiSha       // 七杀
    case zhengGuan   // 正官
    case pianYin     // 偏印
    case zhengYin    // 正印

    var id: String { rawValue }

    var name: String {
        switch self {
        case .biJian: return "比肩"
        case .jieCai: return "劫财"
        case .shiShen: return "食神"
        case .shangGuan: return "伤官"
        case .pianCai: return "偏财"
        case .zhengCai: return "正财"
        case .qiSha: return "七杀"
        case .zhengGuan: return "正官"
        case .pianYin: return "偏印"
        case .zhengYin: return "正印"
        }
    }

    var shortHint: String {
        switch self {
        case .biJian: return "同辈助力、自立进取"
        case .jieCai: return "竞争分夺、行动果断"
        case .shiShen: return "才情表达、轻松享乐"
        case .shangGuan: return "创意突破、口才锋芒"
        case .pianCai: return "偏财机遇、灵活经营"
        case .zhengCai: return "正财稳收、务实积蓄"
        case .qiSha: return "压力挑战、果断决断"
        case .zhengGuan: return "规矩责任、事业名声"
        case .pianYin: return "偏门学问、直觉灵感"
        case .zhengYin: return "贵人学业、滋养庇护"
        }
    }
}

struct PillarDetail: Hashable, Identifiable {
    let kind: PillarKind
    let stemBranch: StemBranch
    let stemGod: TenGod?
    let branchMainGod: TenGod?

    var id: String { kind.rawValue }
    var name: String { stemBranch.name }
}

struct FiveElementBalance: Hashable {
    var scores: [WuXing: Double]

    func score(_ wx: WuXing) -> Double { scores[wx] ?? 0 }

    var strongest: WuXing {
        WuXing.allCases.max(by: { score($0) < score($1) }) ?? .earth
    }

    var weakest: WuXing {
        WuXing.allCases.min(by: { score($0) < score($1) }) ?? .earth
    }

    var summary: String {
        let parts = WuXing.allCases.map { "\($0.name)\(Int(score($0).rounded()))" }
        return parts.joined(separator: " · ")
    }
}

struct BaZiChart: Hashable, Identifiable {
    let id: UUID
    let birthDate: Date
    let gender: Gender
    let timeZoneIdentifier: String
    let year: StemBranch
    let month: StemBranch
    let day: StemBranch
    let hour: StemBranch
    let pillars: [PillarDetail]
    let balance: FiveElementBalance
    let solarTermNote: String

    var dayMaster: HeavenlyStem { day.stem }

    var fourPillarsText: String {
        "\(year.name) \(month.name) \(day.name) \(hour.name)"
    }
}

enum FortunePeriod: String, CaseIterable, Identifiable {
    case year, month, day

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .year: return "流年"
        case .month: return "流月"
        case .day: return "流日"
        }
    }

    var sectionTitle: String {
        switch self {
        case .year: return "流年运势"
        case .month: return "流月运势"
        case .day: return "流日运势"
        }
    }
}

struct FortuneForecast: Hashable, Identifiable {
    let period: FortunePeriod
    let targetDate: Date
    let flowingPillar: StemBranch
    let stemGod: TenGod
    let branchRelations: [String]
    let score: Int
    let tone: String
    let summary: String
    let tips: [String]
    let ruleNotes: [String]

    var id: String { period.rawValue }
}
