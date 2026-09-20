//
//  QuestionTopic.swift
//  QimenDunjia
//
//  「所问之事」分类 → 用神启发式（宫/门/星侧重）。
//  口径：时家常见取用习惯的简化映射，非完整神煞体系；仅供模板解读。
//

import Foundation

enum QuestionTopic: String, CaseIterable, Identifiable, Codable, Hashable {
    case wealth = "求财"
    case travel = "出行"
    case marriage = "婚姻"
    case lawsuit = "诉讼"
    case health = "健康"
    case partnership = "合作"
    case career = "求职"
    case lost = "寻物"
    case general = "综合"

    var id: String { rawValue }

    /// 快捷标签（起局页）
    static var quickTags: [QuestionTopic] {
        [.wealth, .travel, .marriage, .lawsuit, .health, .partnership, .career, .lost]
    }

    var focusHint: String {
        switch self {
        case .wealth: return "侧重生门、景门、开门及财气相关宫"
        case .travel: return "侧重开门、休门与值使落宫"
        case .marriage: return "侧重坤、兑宫及休门、生门"
        case .lawsuit: return "侧重杜门、惊门、伤门与值符"
        case .health: return "侧重天芮/死门相关宫，宜慎看空亡"
        case .partnership: return "侧重六合、开门、生门"
        case .career: return "侧重开门、休门与值符值使"
        case .lost: return "侧重生门、杜门与值使"
        case .general: return "以值符、值使宫为纲"
        }
    }

    /// 关键词命中（自由输入）
    static func detect(from text: String) -> QuestionTopic {
        let t = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !t.isEmpty else { return .general }
        let rules: [(QuestionTopic, [String])] = [
            (.wealth, ["财", "钱", "生意", "投资", "收入", "买卖", "利润", "赚钱"]),
            (.travel, ["出行", "旅游", "出差", "搬家", "远行", "行程", "出国"]),
            (.marriage, ["婚姻", "恋爱", "感情", "桃花", "结婚", "复合", "分手"]),
            (.lawsuit, ["诉讼", "官司", "纠纷", "起诉", "被告", "仲裁"]),
            (.health, ["健康", "病", "身体", "就医", "手术", "康复"]),
            (.partnership, ["合作", "合伙", "协议", "签约", "联营"]),
            (.career, ["工作", "求职", "升迁", "考试", "面试", "调动", "官运"]),
            (.lost, ["失物", "寻找", "丢失", "寻人", "找"])
        ]
        for (topic, keys) in rules {
            if keys.contains(where: { t.contains($0) }) { return topic }
        }
        return .general
    }
}

struct YongShenFocus: Hashable {
    let topic: QuestionTopic
    /// 优先关注的宫（含由门位反查）
    let palaces: [Palace]
    let preferredGates: [EightGate]
    let preferredStars: [NineStar]
    let preferredDeities: [EightDeity]
}

enum YongShenMapping {

    static func focus(for topic: QuestionTopic, cells: [PalaceCell]) -> YongShenFocus {
        let gatePalaces: (EightGate) -> [Palace] = { g in
            cells.compactMap { $0.gate == g ? $0.palace : nil }
        }
        let deityPalaces: (EightDeity) -> [Palace] = { d in
            cells.compactMap { $0.deity == d ? $0.palace : nil }
        }

        switch topic {
        case .wealth:
            var p = gatePalaces(.sheng) + gatePalaces(.jing) + gatePalaces(.kai)
            p.append(contentsOf: [.kun2, .dui7])
            return YongShenFocus(
                topic: topic,
                palaces: unique(p),
                preferredGates: [.sheng, .jing, .kai],
                preferredStars: [.xin, .fu, .ren],
                preferredDeities: [.liuHe, .jiuTian]
            )
        case .travel:
            return YongShenFocus(
                topic: topic,
                palaces: unique(gatePalaces(.kai) + gatePalaces(.xiu) + cells.filter(\.isZhiShi).map(\.palace)),
                preferredGates: [.kai, .xiu],
                preferredStars: [.chong, .fu],
                preferredDeities: [.jiuTian]
            )
        case .marriage:
            return YongShenFocus(
                topic: topic,
                palaces: unique([.kun2, .dui7] + gatePalaces(.xiu) + gatePalaces(.sheng)),
                preferredGates: [.xiu, .sheng],
                preferredStars: [.ren, .xin],
                preferredDeities: [.taiYin, .liuHe]
            )
        case .lawsuit:
            return YongShenFocus(
                topic: topic,
                palaces: unique(gatePalaces(.du) + gatePalaces(.jingShock) + gatePalaces(.shang) + cells.filter(\.isZhiFu).map(\.palace)),
                preferredGates: [.du, .jingShock, .shang],
                preferredStars: [.zhu, .peng],
                preferredDeities: [.baiHu, .xuanWu]
            )
        case .health:
            return YongShenFocus(
                topic: topic,
                palaces: unique(gatePalaces(.si) + cells.compactMap { $0.star == .rui || $0.star == .qin ? $0.palace : nil } + [.kun2, .zhong5]),
                preferredGates: [.si, .jingShock],
                preferredStars: [.rui, .qin],
                preferredDeities: [.tengShe]
            )
        case .partnership:
            return YongShenFocus(
                topic: topic,
                palaces: unique(deityPalaces(.liuHe) + gatePalaces(.kai) + gatePalaces(.sheng)),
                preferredGates: [.kai, .sheng],
                preferredStars: [.fu, .xin],
                preferredDeities: [.liuHe]
            )
        case .career:
            return YongShenFocus(
                topic: topic,
                palaces: unique(gatePalaces(.kai) + gatePalaces(.xiu) + cells.filter { $0.isZhiFu || $0.isZhiShi }.map(\.palace)),
                preferredGates: [.kai, .xiu],
                preferredStars: [.xin, .fu],
                preferredDeities: [.jiuTian, .zhiFu]
            )
        case .lost:
            return YongShenFocus(
                topic: topic,
                palaces: unique(gatePalaces(.sheng) + gatePalaces(.du) + cells.filter(\.isZhiShi).map(\.palace)),
                preferredGates: [.sheng, .du],
                preferredStars: [.ren],
                preferredDeities: [.liuHe]
            )
        case .general:
            return YongShenFocus(
                topic: topic,
                palaces: unique(cells.filter { $0.isZhiFu || $0.isZhiShi }.map(\.palace)),
                preferredGates: [],
                preferredStars: [],
                preferredDeities: [.zhiFu]
            )
        }
    }

    private static func unique(_ palaces: [Palace]) -> [Palace] {
        var seen = Set<Palace>()
        return palaces.filter { seen.insert($0).inserted }
    }
}
