package com.gglee.ziweidoushu.engine

/**
 * 将本命盘投影为大限 / 流年 / 流月视角：
 * - 宫名：以焦点宫为「命」，逆布十二宫
 * - 四化：用对应天干
 */
object ScopeProjector {

    fun natal(chart: ZiWeiChart): Pair<List<PalaceCell>, ScopeOverlay> {
        val flying = FlyingStarAnalyzer.analyzeNatal(chart)
        val hints = (
            FlyingStarAnalyzer.selfHuaOnly(flying) +
                flying.filter {
                    !it.isSelf && (it.fromPalace == PalaceType.MING || it.toPalace == PalaceType.MING)
                }
            ).distinctBy { it.summary }.take(12)
        val overlay = ScopeOverlay(
            scope = ChartScope.NATAL,
            label = "本命盘",
            stem = chart.yearSB.stem,
            siHua = chart.natalSiHua,
            focusBranch = chart.mingBranch,
            flyingHints = hints,
        )
        return chart.cells to overlay
    }

    fun decade(chart: ZiWeiChart, decade: DecadeLimit): Pair<List<PalaceCell>, ScopeOverlay> {
        val focus = decade.branch
        val stem = chart.cellByBranch(focus).stem
        val siHua = SiHuaResolver.resolve(stem, chart.cells)
        val cells = projectCells(chart, focus, siHua)
        val flying = FlyingStarAnalyzer.relatedTo(
            FlyingStarAnalyzer.analyzeNatal(chart),
            ScopeOverlay(ChartScope.DECADE, "", stem, siHua, focus),
        )
        val overlay = ScopeOverlay(
            scope = ChartScope.DECADE,
            label = "大限 ${decade.ageStart}–${decade.ageEnd} · ${decade.palaceType.hanzi}(${focus.hanzi})",
            stem = stem,
            siHua = siHua,
            focusBranch = focus,
            flyingHints = flying.take(12),
        )
        return cells to overlay
    }

    /** 流年：流年地支宫为流年命宫；四化用流年天干。 */
    fun annual(
        chart: ZiWeiChart,
        yearStemBranch: StemBranch,
        solarYear: Int,
    ): Pair<List<PalaceCell>, ScopeOverlay> {
        val focus = yearStemBranch.branch
        val siHua = SiHuaResolver.resolve(yearStemBranch.stem, chart.cells)
        val cells = projectCells(chart, focus, siHua)
        val flying = FlyingStarAnalyzer.relatedTo(
            FlyingStarAnalyzer.analyzeNatal(chart),
            ScopeOverlay(ChartScope.ANNUAL, "", yearStemBranch.stem, siHua, focus),
        )
        val overlay = ScopeOverlay(
            scope = ChartScope.ANNUAL,
            label = "流年 $solarYear ${yearStemBranch.name}",
            stem = yearStemBranch.stem,
            siHua = siHua,
            focusBranch = focus,
            flyingHints = flying.take(12),
        )
        return cells to overlay
    }

    /**
     * 流月：以流年地支为正月起点顺数至农历月 → 流月命宫；
     * 月干用流年干五虎遁。
     */
    fun monthly(
        chart: ZiWeiChart,
        yearStemBranch: StemBranch,
        solarYear: Int,
        lunarMonth: Int,
    ): Pair<List<PalaceCell>, ScopeOverlay> {
        val month = lunarMonth.coerceIn(1, 12)
        val focus = EarthlyBranch.from(yearStemBranch.branch.rawValue + (month - 1))
        val monthStem = monthStemFromYear(yearStemBranch.stem, month)
        val siHua = SiHuaResolver.resolve(monthStem, chart.cells)
        val cells = projectCells(chart, focus, siHua)
        val flying = FlyingStarAnalyzer.relatedTo(
            FlyingStarAnalyzer.analyzeNatal(chart),
            ScopeOverlay(ChartScope.MONTHLY, "", monthStem, siHua, focus),
        )
        val overlay = ScopeOverlay(
            scope = ChartScope.MONTHLY,
            label = "流月 $solarYear 农历${month}月 · ${monthStem.hanzi}${focus.hanzi}",
            stem = monthStem,
            siHua = siHua,
            focusBranch = focus,
            flyingHints = flying.take(12),
        )
        return cells to overlay
    }

    /** 五虎遁：年干起寅月干，顺推到农历月 */
    fun monthStemFromYear(yearStem: HeavenlyStem, lunarMonth: Int): HeavenlyStem {
        val yinStem = when (yearStem) {
            HeavenlyStem.JIA, HeavenlyStem.JI -> HeavenlyStem.BING
            HeavenlyStem.YI, HeavenlyStem.GENG -> HeavenlyStem.WU
            HeavenlyStem.BING, HeavenlyStem.XIN -> HeavenlyStem.GENG
            HeavenlyStem.DING, HeavenlyStem.REN -> HeavenlyStem.REN
            HeavenlyStem.WU, HeavenlyStem.GUI -> HeavenlyStem.JIA
        }
        return HeavenlyStem.from(yinStem.rawValue + (lunarMonth - 1))
    }

    private fun projectCells(
        chart: ZiWeiChart,
        focusBranch: EarthlyBranch,
        scopeSiHua: List<SiHuaEntry>,
    ): List<PalaceCell> {
        val scopeByStar = scopeSiHua.associate { it.star to it.siHua }
        fun scopeTypeFor(branch: EarthlyBranch): PalaceType {
            val steps = (focusBranch.rawValue - branch.rawValue + 12) % 12
            return PalaceType.entries[steps]
        }
        return chart.cells.map { cell ->
            cell.copy(
                scopeType = scopeTypeFor(cell.branch),
                stars = cell.stars.map { ps ->
                    ps.copy(scopeSiHua = scopeByStar[ps.star])
                },
            )
        }
    }
}
