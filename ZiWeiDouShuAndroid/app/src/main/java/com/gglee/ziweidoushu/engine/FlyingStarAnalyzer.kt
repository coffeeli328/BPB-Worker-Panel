package com.gglee.ziweidoushu.engine

/**
 * 飞星 / 自化：以各宫天干发出四化，看落星所在宫。
 * 本命飞星用本命十二宫干；大限/流年视角也可对「当前焦点宫干」做专项，
 * 这里默认算完整本命十二宫飞化一览，并标出自化。
 */
object FlyingStarAnalyzer {

    fun analyzeNatal(chart: ZiWeiChart): List<FlyingHint> {
        val starLoc = mutableMapOf<Star, Pair<PalaceType, EarthlyBranch>>()
        chart.cells.forEach { cell ->
            cell.stars.forEach { ps ->
                starLoc[ps.star] = cell.type to cell.branch
            }
        }
        val hints = mutableListOf<FlyingHint>()
        chart.cells.forEach { cell ->
            val map = SiHuaResolver.starsFor(cell.stem)
            for (sh in SiHua.entries) {
                val star = map[sh] ?: continue
                val loc = starLoc[star] ?: continue
                val (toType, toBranch) = loc
                hints += FlyingHint(
                    fromPalace = cell.type,
                    fromBranch = cell.branch,
                    fromStem = cell.stem,
                    siHua = sh,
                    star = star,
                    toPalace = toType,
                    toBranch = toBranch,
                    isSelf = toBranch == cell.branch,
                )
            }
        }
        return hints
    }

    /** 与当前 scope 四化相关的飞化：发出干 = overlay.stem 的宫，或落点在焦点宫 */
    fun relatedTo(
        all: List<FlyingHint>,
        overlay: ScopeOverlay,
    ): List<FlyingHint> {
        val stem = overlay.stem ?: return all.filter { it.isSelf }.take(8)
        val focus = overlay.focusBranch
        return all.filter { h ->
            h.fromStem == stem ||
                (focus != null && (h.fromBranch == focus || h.toBranch == focus)) ||
                (overlay.scope == ChartScope.NATAL && h.isSelf)
        }.distinctBy { "${it.fromBranch}-${it.siHua}-${it.star}-${it.toBranch}" }
    }

    fun selfHuaOnly(all: List<FlyingHint>): List<FlyingHint> =
        all.filter { it.isSelf }
}
