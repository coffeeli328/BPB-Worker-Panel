package com.gglee.qimendunjia.engine

import java.util.Date
import java.util.TimeZone

object JuResolver {

    val termJu: Map<String, Pair<Boolean, List<Int>>> = mapOf(
        "冬至" to Pair(true, listOf(1, 7, 4)),
        "惊蛰" to Pair(true, listOf(1, 7, 4)),
        "小寒" to Pair(true, listOf(2, 8, 5)),
        "大寒" to Pair(true, listOf(3, 9, 6)),
        "春分" to Pair(true, listOf(3, 9, 6)),
        "立春" to Pair(true, listOf(8, 5, 2)),
        "雨水" to Pair(true, listOf(9, 6, 3)),
        "清明" to Pair(true, listOf(4, 1, 7)),
        "立夏" to Pair(true, listOf(4, 1, 7)),
        "谷雨" to Pair(true, listOf(5, 2, 8)),
        "小满" to Pair(true, listOf(5, 2, 8)),
        "芒种" to Pair(true, listOf(6, 3, 9)),
        "夏至" to Pair(false, listOf(9, 3, 6)),
        "白露" to Pair(false, listOf(9, 3, 6)),
        "小暑" to Pair(false, listOf(8, 2, 5)),
        "大暑" to Pair(false, listOf(7, 1, 4)),
        "秋分" to Pair(false, listOf(7, 1, 4)),
        "立秋" to Pair(false, listOf(2, 5, 8)),
        "处暑" to Pair(false, listOf(1, 4, 7)),
        "寒露" to Pair(false, listOf(6, 9, 3)),
        "立冬" to Pair(false, listOf(6, 9, 3)),
        "霜降" to Pair(false, listOf(5, 8, 2)),
        "小雪" to Pair(false, listOf(5, 8, 2)),
        "大雪" to Pair(false, listOf(4, 7, 1)),
    )

    fun termJuEntry(name: String): Pair<Boolean, List<Int>> {
        val key = name.replace("（闰）", "")
        return termJu[key] ?: Pair(true, listOf(1, 7, 4))
    }

    fun fuTou(forDay: StemBranch): StemBranch {
        var idx = forDay.sexagenaryIndex
        repeat(10) {
            val sb = StemBranch.from(idx)
            if (sb.stem == HeavenlyStem.JIA || sb.stem == HeavenlyStem.JI) return sb
            idx = (idx + 59) % 60
        }
        return forDay
    }

    fun yuanIndex(fuTou: StemBranch): Int = when (fuTou.branch) {
        EarthlyBranch.ZI, EarthlyBranch.WU, EarthlyBranch.MAO, EarthlyBranch.YOU -> 0
        EarthlyBranch.YIN, EarthlyBranch.SHEN, EarthlyBranch.SI, EarthlyBranch.HAI -> 1
        EarthlyBranch.CHEN, EarthlyBranch.XU, EarthlyBranch.CHOU, EarthlyBranch.WEI -> 2
    }

    fun yuanName(index: Int): String = arrayOf("上元", "中元", "下元")[index.coerceIn(0, 2)]

    fun resolve(
        day: StemBranch,
        queryDate: Date,
        timeZone: TimeZone,
        method: JuMethod,
    ): JuResolution = when (method) {
        JuMethod.CHAI_BU -> {
            val term = SolarTerms.currentTerm(forDate = queryDate, timeZone = timeZone)
            resolveChaibu(day = day, solarTermName = term.name).copy(
                juMethod = JuMethod.CHAI_BU,
                solarTermInstant = term.approximateDate,
                phase = ZhiYunPhase.ZHENG_SHOU,
                isRunQi = false,
            )
        }
        JuMethod.ZHI_YUN -> ZhiYunResolver.resolve(queryDate = queryDate, day = day, timeZone = timeZone)
    }

    fun resolveChaibu(day: StemBranch, solarTermName: String): JuResolution {
        val ft = fuTou(day)
        val yi = yuanIndex(ft)
        val entry = termJuEntry(solarTermName)
        return JuResolution(
            isYangDun = entry.first,
            juNumber = entry.second[yi],
            yuanName = yuanName(yi),
            yuanIndex = yi,
            solarTermName = solarTermName,
            fuTou = ft,
            juMethod = JuMethod.CHAI_BU,
            phase = ZhiYunPhase.ZHENG_SHOU,
            isRunQi = false,
            solarTermInstant = null,
        )
    }

    fun resolve(day: StemBranch, solarTermName: String): JuResolution =
        resolveChaibu(day, solarTermName)

    fun ju(term: String, yuanIndex: Int): Pair<Boolean, Int>? {
        val e = termJu[term] ?: return null
        if (yuanIndex !in 0..2) return null
        return Pair(e.first, e.second[yuanIndex])
    }
}
