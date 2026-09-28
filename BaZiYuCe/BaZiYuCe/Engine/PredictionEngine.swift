//
//  PredictionEngine.swift
//  BaZiYuCe
//
//  规则透明的流年 / 流月 / 流日预测。可扩展：增补十神模板与关系加减分。
//

import Foundation

enum PredictionEngine {

    static func forecast(
        chart: BaZiChart,
        period: FortunePeriod,
        at date: Date = Date(),
        timeZone: TimeZone = .current
    ) -> FortuneForecast {
        let flowing: StemBranch
        switch period {
        case .year:
            flowing = GanzhiCalendar.flowingYear(for: date, timeZone: timeZone)
        case .month:
            flowing = GanzhiCalendar.flowingMonth(for: date, timeZone: timeZone)
        case .day:
            flowing = GanzhiCalendar.flowingDay(for: date, timeZone: timeZone)
        }

        let dayMaster = chart.dayMaster
        let stemGod = TenGods.of(stem: flowing.stem, dayMaster: dayMaster)
        let natalBranches = [chart.year.branch, chart.month.branch, chart.day.branch, chart.hour.branch]
        let relations = BranchRelations.describe(flowing: flowing.branch, natal: natalBranches)

        var score = baseScore(for: stemGod)
        var ruleNotes: [String] = [
            "流\(period.displayName.suffix(1))干\(flowing.stem.name)相对日主\(dayMaster.name)为\(stemGod.name)"
        ]

        for r in relations {
            if r.contains("冲") {
                score -= 8
                ruleNotes.append("关系 \(r)：动荡减分")
            } else if r.contains("刑") || r.contains("害") {
                score -= 5
                ruleNotes.append("关系 \(r)：摩擦减分")
            } else if r.contains("合") {
                score += 6
                ruleNotes.append("关系 \(r)：和合加分")
            }
        }

        // 五行：流干五行若补原局最弱，略加分；若加重最旺，略减分（求中和）
        let flowWX = flowing.stem.wuXing
        if flowWX == chart.balance.weakest {
            score += 5
            ruleNotes.append("流干\(flowWX.name)补原局偏弱之\(flowWX.name)")
        } else if flowWX == chart.balance.strongest {
            score -= 3
            ruleNotes.append("流干\(flowWX.name)加重原局偏旺之\(flowWX.name)")
        }

        score = min(95, max(25, score))

        let tone = toneLabel(score: score)
        let summary = summaryText(period: period, flowing: flowing, god: stemGod, tone: tone, relations: relations)
        let tips = tipsText(period: period, god: stemGod, relations: relations, balance: chart.balance, gender: chart.gender)

        return FortuneForecast(
            period: period,
            targetDate: date,
            flowingPillar: flowing,
            stemGod: stemGod,
            branchRelations: relations,
            score: score,
            tone: tone,
            summary: summary,
            tips: tips,
            ruleNotes: ruleNotes
        )
    }

    static func allForecasts(
        chart: BaZiChart,
        at date: Date = Date(),
        timeZone: TimeZone = .current
    ) -> [FortuneForecast] {
        FortunePeriod.allCases.map { forecast(chart: chart, period: $0, at: date, timeZone: timeZone) }
    }

    // MARK: - Scoring & copy

    private static func baseScore(for god: TenGod) -> Int {
        switch god {
        case .zhengYin, .shiShen, .zhengCai: return 72
        case .pianCai, .zhengGuan, .biJian: return 68
        case .pianYin, .jieCai: return 62
        case .shangGuan: return 58
        case .qiSha: return 55
        }
    }

    private static func toneLabel(score: Int) -> String {
        switch score {
        case 80...: return "较顺"
        case 68..<80: return "平稳偏喜"
        case 55..<68: return "起伏参半"
        default: return "需谨慎收敛"
        }
    }

    private static func summaryText(
        period: FortunePeriod,
        flowing: StemBranch,
        god: TenGod,
        tone: String,
        relations: [String]
    ) -> String {
        let rel = relations.isEmpty ? "与原局地支无明显刑冲合害" : "见" + relations.joined(separator: "、")
        let span: String
        switch period {
        case .year: span = "这一年"
        case .month: span = "这个月"
        case .day: span = "这一天"
        }
        return "\(span)干支为\(flowing.name)，天干十神属\(god.name)（\(god.shortHint)）。整体倾向「\(tone)」。地支方面：\(rel)。"
    }

    private static func tipsText(
        period: FortunePeriod,
        god: TenGod,
        relations: [String],
        balance: FiveElementBalance,
        gender: Gender
    ) -> [String] {
        var tips: [String] = []

        switch god {
        case .zhengGuan, .qiSha:
            tips.append(period == .day
                ? "适合处理规章事务、会议与责任事项；避免硬碰硬争执。"
                : "事业与责任议题升温，宜守纪律、重承诺，重大决定留书面记录。")
        case .zhengCai, .pianCai:
            tips.append("财务上宜开源节流并举；大额投资先做风险评估，勿跟风。")
        case .zhengYin, .pianYin:
            tips.append("利于学习、考证、求教贵人；注意作息与脾胃调养。")
        case .shiShen, .shangGuan:
            tips.append("创意与表达有光彩，适合分享成果；对外发言注意分寸，避免口无遮拦。")
        case .biJian, .jieCai:
            tips.append("人际协作与竞争并存，合作前先谈清边界与分成。")
        }

        if relations.contains(where: { $0.contains("冲") }) {
            tips.append("有冲则多变动：行程、合约、搬家类事务预留弹性，别赶在交节当日硬上。")
        }
        if relations.contains(where: { $0.contains("合") }) {
            tips.append("有合则易遇牵线：适合洽谈合作、修复关系，但勿匆忙签字画押。")
        }

        let weak = balance.weakest.name
        let strong = balance.strongest.name
        tips.append("原局五行相对偏弱在「\(weak)」、偏旺在「\(strong)」。日常可从作息、环境、事务类型上略补\(weak)、疏\(strong)。")

        switch period {
        case .year:
            tips.append("流年看大方向：把年度目标拆成季度节点，每季复盘一次即可。")
        case .month:
            tips.append("流月看节奏：本月聚焦 1–2 件关键事项，比面面俱到更有效。")
        case .day:
            tips.append("流日看当下：今天把最重要的一件事做完，比 multitask 更稳。")
        }

        _ = gender // 预留：后续可按性别调整大运顺逆相关提示
        return tips
    }
}
