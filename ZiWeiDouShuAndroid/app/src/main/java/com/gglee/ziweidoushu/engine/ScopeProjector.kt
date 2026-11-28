package com.gglee.ziweidoushu.engine

/**
 * 将本命盘投影为大限 / 流年视角：
 * - 宫名：以焦点宫为「命」，逆布十二宫
 * - 四化：用大限宫干 / 流年干
 */
object ScopeProjector {

    fun natal(chart: ZiWeiChart): Pair<List<PalaceCell>, ScopeOverlay> {
        val overlay = ScopeOverlay(
            scope = ChartScope.NATAL,
            label = "本命盘",
            stem = chart.yearSB.stem,
            siHua = chart.natalSiHua,
            focusBranch = chart.mingBranch,
        )
        return chart.cells to overlay
    }

    fun decade(chart: ZiWeiChart, decade: DecadeLimit): Pair<List<PalaceCell>, ScopeOverlay> {
        val focus = decade.branch
        val stem = chart.cellByBranch(focus).stem
        val siHua = SiHuaResolver.resolve(stem, chart.cells)
        val cells = projectCells(chart, focus, siHua)
        val overlay = ScopeOverlay(
            scope = ChartScope.DECADE,
            label = "大限 ${decade.ageStart}–${decade.ageEnd} · ${decade.palaceType.hanzi}(${focus.hanzi})",
            stem = stem,
            siHua = siHua,
            focusBranch = focus,
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
        val overlay = ScopeOverlay(
            scope = ChartScope.ANNUAL,
            label = "流年 $solarYear ${yearStemBranch.name}",
            stem = yearStemBranch.stem,
            siHua = siHua,
            focusBranch = focus,
        )
        return cells to overlay
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
