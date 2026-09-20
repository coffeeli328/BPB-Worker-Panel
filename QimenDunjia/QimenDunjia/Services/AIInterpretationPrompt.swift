//
//  AIInterpretationPrompt.swift
//  QimenDunjia
//
//  Builds system + user messages for OpenAI-compatible chat completions.
//

import Foundation

enum AIInterpretationPrompt {

    static let systemMessage = """
    你是时家奇门遁甲学习助手。根据用户提供的「所问之事」与结构化盘面作针对性解读。
    硬性要求：
    1. 仅供参考，不保证吉凶，不作绝对预言或医疗/法律/投资承诺。
    2. 必须针对所问之事，结合用神相关宫与值符值使，具体分析，避免空话套话。
    3. 使用简体中文，按以下四段输出（用小标题）：
       【总断】【有利因素】【风险】【行动建议】
    4. 若所问为空，先说明应补问，再给极简盘面纲要。
    """

    /// Stable cache key: question + chart identity fields + model.
    static func cacheKey(chart: QimenChart, model: String) -> String {
        var parts: [String] = [
            chart.id.uuidString,
            chart.question,
            chart.juTitle,
            chart.ganzhiLine,
            chart.zhiFuStar.name,
            chart.zhiFuPalace.name,
            chart.zhiShiGate.displayName,
            chart.zhiShiPalace.name,
            chart.xunKong.map(\.name).joined(),
            model
        ]
        for c in chart.cells.sorted(by: { $0.palace.rawValue < $1.palace.rawValue }) {
            parts.append([
                "\(c.palace.rawValue)",
                c.heavenStem?.name ?? "",
                c.earthStem?.name ?? "",
                c.star?.shortName ?? "",
                c.gate?.displayName ?? "",
                c.deity?.shortName ?? "",
                c.isEmpty ? "空" : "",
                c.isZhiFu ? "符" : "",
                c.isZhiShi ? "使" : ""
            ].joined(separator: ","))
        }
        return parts.joined(separator: "|")
    }

    static func userPayload(chart: QimenChart) -> String {
        let focus = YongShenMapping.focus(for: chart.questionTopic, cells: chart.cells)
        let focusNames = focus.palaces.map { "\($0.name)\($0.rawValue)" }.joined(separator: "、")
        let gateHint = focus.preferredGates.map(\.displayName).joined(separator: "、")
        let starHint = focus.preferredStars.map(\.name).joined(separator: "、")

        var lines: [String] = []
        lines.append("【所问之事】\(chart.hasQuestion ? chart.question : "（未填写）")")
        lines.append("【事项归类】\(chart.questionTopic.rawValue) — \(chart.questionTopic.focusHint)")
        lines.append("【用神提示宫】\(focusNames.isEmpty ? "值符/值使" : focusNames)")
        if !gateHint.isEmpty { lines.append("【事门侧重】\(gateHint)") }
        if !starHint.isEmpty { lines.append("【事星侧重】\(starHint)") }
        lines.append("【定局】\(chart.juTitle) · \(chart.solarTermName) · \(chart.juPhase.rawValue)")
        lines.append("【干支】\(chart.ganzhiLine)")
        lines.append("【值符】\(chart.zhiFuStar.name)在\(chart.zhiFuPalace.name)宫")
        lines.append("【值使】\(chart.zhiShiGate.displayName)在\(chart.zhiShiPalace.name)宫")
        lines.append("【旬空】\(chart.xunKong.map(\.name).joined(separator: ""))")
        lines.append("【九宫】（宫|天盘干|地盘干|星|门|神|空亡|值符|值使）")
        for c in chart.cells.sorted(by: { $0.palace.rawValue < $1.palace.rawValue }) {
            let marks = [
                c.isEmpty ? "空" : nil,
                c.isZhiFu ? "值符" : nil,
                c.isZhiShi ? "值使" : nil
            ].compactMap { $0 }.joined(separator: "/")
            lines.append([
                "\(c.palace.name)\(c.palace.rawValue)",
                c.heavenStem?.name ?? "·",
                c.earthStem?.name ?? "·",
                c.star?.name ?? "·",
                c.gate?.displayName ?? "·",
                c.deity?.name(isYangDun: chart.isYangDun) ?? "·",
                marks.isEmpty ? "-" : marks
            ].joined(separator: "|"))
        }
        lines.append("请严格按系统要求的四段结构，针对「\(chart.hasQuestion ? chart.question : "未填问题")」给出明确解读。")
        return lines.joined(separator: "\n")
    }
}
