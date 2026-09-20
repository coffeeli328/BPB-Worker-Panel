//
//  InterpretationEngine.swift
//  QimenDunjia
//
//  针对「所问之事」的规则/启发式模板解读。非 AI、非吉凶保证。
//

import Foundation

enum InterpretationEngine {

    static func build(
        question: String,
        isYang: Bool,
        ju: Int,
        zhiFu: NineStar,
        zhiShi: EightGate,
        zhiFuPalace: Palace,
        zhiShiPalace: Palace,
        cells: [PalaceCell],
        hour: StemBranch,
        xunKong: [EarthlyBranch],
        isYangDun: Bool
    ) -> [InterpretationItem] {
        let q = question.trimmingCharacters(in: .whitespacesAndNewlines)
        if q.isEmpty {
            return [
                InterpretationItem(
                    title: "请先填写所问之事",
                    detail: "起局页填写具体问题（如求财、出行、婚姻）后，解读会按用神宫位针对该问题展开。空问只能给空泛总论，意义有限。",
                    tone: .caution
                ),
                InterpretationItem(
                    title: "局象（未绑定问题）",
                    detail: "\(isYang ? "阳遁" : "阴遁")\(ju)局。值符\(zhiFu.name)在\(zhiFuPalace.name)宫，值使\(zhiShi.displayName)在\(zhiShiPalace.name)宫。请返回填写问题后重新排盘。",
                    tone: .neutral
                )
            ]
        }

        let topic = QuestionTopic.detect(from: q)
        let focus = YongShenMapping.focus(for: topic, cells: cells)
        let focusCells = focus.palaces.compactMap { p in cells.first { $0.palace == p } }

        var items: [InterpretationItem] = []

        items.append(InterpretationItem(
            title: "所问之事",
            detail: "「\(q)」→ 归类为「\(topic.rawValue)」。\(topic.focusHint)。以下为规则模板，仅供学习参考，不作决策保证。",
            tone: .neutral
        ))

        // 相关宫位要点
        let palaceNotes = focusCells.prefix(4).map { cell -> String in
            describeCell(cell, isYangDun: isYangDun, focus: focus)
        }.joined(separator: "\n")
        items.append(InterpretationItem(
            title: "与所问相关的宫位要点",
            detail: palaceNotes.isEmpty
                ? "未能定位明确用神宫，改看值符\(zhiFuPalace.name)与值使\(zhiShiPalace.name)。"
                : palaceNotes,
            tone: .neutral
        ))

        // 宜 / 慎
        let (yi, shen, score) = yiShen(
            focus: focus,
            focusCells: Array(focusCells),
            zhiFu: zhiFu,
            zhiShi: zhiShi,
            zhiFuPalace: zhiFuPalace,
            cells: cells
        )
        items.append(InterpretationItem(
            title: "宜",
            detail: yi,
            tone: .auspicious
        ))
        items.append(InterpretationItem(
            title: "慎",
            detail: shen,
            tone: .caution
        ))

        // 一句话结论
        items.append(InterpretationItem(
            title: "结合问题的一句话结论",
            detail: conclusion(question: q, topic: topic, score: score, zhiShi: zhiShi, focusCells: Array(focusCells)),
            tone: score >= 1 ? .auspicious : (score <= -1 ? .caution : .neutral)
        ))

        items.append(InterpretationItem(
            title: "声明",
            detail: "解读由宫、门、星、神与空亡的启发式规则生成，非人工断验，亦非吉凶保证。重要用事请交叉历书并自行判断。",
            tone: .neutral
        ))

        return items
    }

    // MARK: - Helpers

    private static func describeCell(_ cell: PalaceCell, isYangDun: Bool, focus: YongShenFocus) -> String {
        let gate = cell.gate?.displayName ?? "—"
        let star = cell.star?.name ?? "—"
        let deity = cell.deity?.name(isYangDun: isYangDun) ?? "—"
        let h = cell.heavenStem?.name ?? "·"
        let e = cell.earthStem?.name ?? "·"
        var marks: [String] = []
        if cell.isZhiFu { marks.append("值符") }
        if cell.isZhiShi { marks.append("值使") }
        if cell.isEmpty { marks.append("空亡") }
        if let g = cell.gate, focus.preferredGates.contains(g) { marks.append("事门") }
        if let s = cell.star, focus.preferredStars.contains(s) { marks.append("事星") }
        let mark = marks.isEmpty ? "" : "〔\(marks.joined(separator: "·"))〕"
        return "\(cell.palace.name)\(cell.palace.rawValue)宫\(mark)：\(star)、\(gate)、\(deity)；天\(h)/地\(e)。"
    }

    private static func yiShen(
        focus: YongShenFocus,
        focusCells: [PalaceCell],
        zhiFu: NineStar,
        zhiShi: EightGate,
        zhiFuPalace: Palace,
        cells: [PalaceCell]
    ) -> (String, String, Int) {
        var score = 0
        var yiParts: [String] = []
        var shenParts: [String] = []

        let goodGateHit = focusCells.contains { cell in
            guard let g = cell.gate else { return false }
            return focus.preferredGates.contains(g) && g.isAuspicious && !cell.isEmpty
        }
        if goodGateHit {
            score += 1
            yiParts.append("事相关吉门落宫且未空，传统上利于推进所问。")
        }

        if zhiShi.isAuspicious {
            score += 1
            yiParts.append("值使\(zhiShi.displayName)较利行动节奏。")
        } else {
            score -= 1
            shenParts.append("值使\(zhiShi.displayName)偏滞，办事宜缓、留余地。")
        }

        if zhiFu.isAuspicious {
            score += 1
            yiParts.append("值符\(zhiFu.name)得力，主事人气场相对有助。")
        }

        if focusCells.contains(where: \.isEmpty) {
            score -= 1
            shenParts.append("用神相关宫见空亡，事易虚实不定，勿过度承诺。")
        }

        if let zfCell = cells.first(where: { $0.palace == zhiFuPalace }),
           zfCell.star?.innatePalace == zhiFuPalace {
            score -= 1
            shenParts.append("值符有伏吟倾向，所问或反复胶着，宜守不宜急。")
        }

        // 诉讼类：杜门可反为「宜藏」
        if focus.topic == .lawsuit {
            if focusCells.contains(where: { $0.gate == .du }) {
                yiParts.append("杜门临事宫，传统主宜守密、少张扬。")
            }
        }

        if yiParts.isEmpty {
            yiParts.append("暂无明显「宜进」信号，可先观察用神宫变动与时机。")
        }
        if shenParts.isEmpty {
            shenParts.append("未见强烈凶咎模板信号，仍须结合现实条件。")
        }

        return (yiParts.joined(separator: " "), shenParts.joined(separator: " "), score)
    }

    private static func conclusion(
        question: String,
        topic: QuestionTopic,
        score: Int,
        zhiShi: EightGate,
        focusCells: [PalaceCell]
    ) -> String {
        let empty = focusCells.contains(where: \.isEmpty)
        let tone: String
        if score >= 2 && !empty {
            tone = "就「\(question)」而言，盘面偏可尝试推进"
        } else if score <= -1 || empty {
            tone = "就「\(question)」而言，盘面偏宜谨慎、缓图或改期"
        } else {
            tone = "就「\(question)」而言，盘面中平，宜小步验证"
        }
        let gateNote = "（值使\(zhiShi.displayName)，事项归类：\(topic.rawValue)）"
        return tone + gateNote + "。请以现实筹划为准。"
    }
}
