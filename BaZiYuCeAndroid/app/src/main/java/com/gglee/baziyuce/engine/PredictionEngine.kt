package com.gglee.baziyuce.engine

import java.util.Date
import java.util.TimeZone

/** 规则透明的流年 / 流月 / 流日预测。与 iOS PredictionEngine 同模板。 */
object PredictionEngine {

    fun forecast(
        chart: BaZiChart,
        period: FortunePeriod,
        at: Date = Date(),
        timeZone: TimeZone = TimeZone.getDefault(),
    ): FortuneForecast {
        val flowing = when (period) {
            FortunePeriod.YEAR -> GanzhiCalendar.flowingYear(at, timeZone)
            FortunePeriod.MONTH -> GanzhiCalendar.flowingMonth(at, timeZone)
            FortunePeriod.DAY -> GanzhiCalendar.flowingDay(at, timeZone)
        }

        val dayMaster = chart.dayMaster
        val stemGod = TenGods.of(flowing.stem, dayMaster)
        val natalBranches = listOf(chart.year.branch, chart.month.branch, chart.day.branch, chart.hour.branch)
        val relations = BranchRelations.describe(flowing.branch, natalBranches)

        var score = baseScore(stemGod)
        val ruleNotes = mutableListOf(
            "流${period.displayName.last()}干${flowing.stem.displayName}相对日主${dayMaster.displayName}为${stemGod.displayName}",
        )

        for (r in relations) {
            when {
                "冲" in r -> {
                    score -= 8
                    ruleNotes += "关系 $r：动荡减分"
                }
                "刑" in r || "害" in r -> {
                    score -= 5
                    ruleNotes += "关系 $r：摩擦减分"
                }
                "合" in r -> {
                    score += 6
                    ruleNotes += "关系 $r：和合加分"
                }
            }
        }

        val flowWX = flowing.stem.wuXing
        when (flowWX) {
            chart.balance.weakest -> {
                score += 5
                ruleNotes += "流干${flowWX.displayName}补原局偏弱之${flowWX.displayName}"
            }
            chart.balance.strongest -> {
                score -= 3
                ruleNotes += "流干${flowWX.displayName}加重原局偏旺之${flowWX.displayName}"
            }
            else -> Unit
        }

        score = score.coerceIn(25, 95)

        val tone = toneLabel(score)
        val summary = summaryText(period, flowing, stemGod, tone, relations)
        val tips = tipsText(period, stemGod, relations, chart.balance, chart.gender)

        return FortuneForecast(
            period = period,
            targetDate = at,
            flowingPillar = flowing,
            stemGod = stemGod,
            branchRelations = relations,
            score = score,
            tone = tone,
            summary = summary,
            tips = tips,
            ruleNotes = ruleNotes,
        )
    }

    fun allForecasts(
        chart: BaZiChart,
        at: Date = Date(),
        timeZone: TimeZone = TimeZone.getDefault(),
    ): List<FortuneForecast> =
        FortunePeriod.entries.map { forecast(chart, it, at, timeZone) }

    private fun baseScore(god: TenGod): Int = when (god) {
        TenGod.ZHENG_YIN, TenGod.SHI_SHEN, TenGod.ZHENG_CAI -> 72
        TenGod.PIAN_CAI, TenGod.ZHENG_GUAN, TenGod.BI_JIAN -> 68
        TenGod.PIAN_YIN, TenGod.JIE_CAI -> 62
        TenGod.SHANG_GUAN -> 58
        TenGod.QI_SHA -> 55
    }

    private fun toneLabel(score: Int): String = when {
        score >= 80 -> "较顺"
        score >= 68 -> "平稳偏喜"
        score >= 55 -> "起伏参半"
        else -> "需谨慎收敛"
    }

    private fun summaryText(
        period: FortunePeriod,
        flowing: StemBranch,
        god: TenGod,
        tone: String,
        relations: List<String>,
    ): String {
        val rel = if (relations.isEmpty()) {
            "与原局地支无明显刑冲合害"
        } else {
            "见" + relations.joinToString("、")
        }
        val span = when (period) {
            FortunePeriod.YEAR -> "这一年"
            FortunePeriod.MONTH -> "这个月"
            FortunePeriod.DAY -> "这一天"
        }
        return "${span}干支为${flowing.name}，天干十神属${god.displayName}（${god.shortHint}）。整体倾向「$tone」。地支方面：$rel。"
    }

    private fun tipsText(
        period: FortunePeriod,
        god: TenGod,
        relations: List<String>,
        balance: FiveElementBalance,
        gender: Gender,
    ): List<String> {
        val tips = mutableListOf<String>()

        when (god) {
            TenGod.ZHENG_GUAN, TenGod.QI_SHA -> tips += if (period == FortunePeriod.DAY) {
                "适合处理规章事务、会议与责任事项；避免硬碰硬争执。"
            } else {
                "事业与责任议题升温，宜守纪律、重承诺，重大决定留书面记录。"
            }
            TenGod.ZHENG_CAI, TenGod.PIAN_CAI ->
                tips += "财务上宜开源节流并举；大额投资先做风险评估，勿跟风。"
            TenGod.ZHENG_YIN, TenGod.PIAN_YIN ->
                tips += "利于学习、考证、求教贵人；注意作息与脾胃调养。"
            TenGod.SHI_SHEN, TenGod.SHANG_GUAN ->
                tips += "创意与表达有光彩，适合分享成果；对外发言注意分寸，避免口无遮拦。"
            TenGod.BI_JIAN, TenGod.JIE_CAI ->
                tips += "人际协作与竞争并存，合作前先谈清边界与分成。"
        }

        if (relations.any { "冲" in it }) {
            tips += "有冲则多变动：行程、合约、搬家类事务预留弹性，别赶在交节当日硬上。"
        }
        if (relations.any { "合" in it }) {
            tips += "有合则易遇牵线：适合洽谈合作、修复关系，但勿匆忙签字画押。"
        }

        val weak = balance.weakest.displayName
        val strong = balance.strongest.displayName
        tips += "原局五行相对偏弱在「$weak」、偏旺在「$strong」。日常可从作息、环境、事务类型上略补$weak、疏$strong。"

        tips += when (period) {
            FortunePeriod.YEAR -> "流年看大方向：把年度目标拆成季度节点，每季复盘一次即可。"
            FortunePeriod.MONTH -> "流月看节奏：本月聚焦 1–2 件关键事项，比面面俱到更有效。"
            FortunePeriod.DAY -> "流日看当下：今天把最重要的一件事做完，比 multitask 更稳。"
        }

        @Suppress("UNUSED_VARIABLE")
        val unusedGender = gender
        return tips
    }
}
