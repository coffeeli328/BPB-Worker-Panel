package com.gglee.baziyuce.engine

/** 简易五行力量：天干满分 + 地支藏干加权。 */
object FiveElements {

    private val hiddenWeights = listOf(1.0, 0.5, 0.25)

    fun balance(
        year: StemBranch,
        month: StemBranch,
        day: StemBranch,
        hour: StemBranch,
    ): FiveElementBalance {
        val scores = WuXing.entries.associateWith { 0.0 }.toMutableMap()

        fun addStem(stem: HeavenlyStem, weight: Double) {
            scores[stem.wuXing] = (scores[stem.wuXing] ?: 0.0) + weight
        }

        fun addBranch(branch: EarthlyBranch, monthBoost: Double) {
            branch.hiddenStems.forEachIndexed { i, s ->
                val w = (hiddenWeights.getOrElse(i) { 0.2 }) * monthBoost
                addStem(s, w)
            }
        }

        val monthElement = month.branch.wuXing
        for (pillar in listOf(year, month, day, hour)) {
            addStem(pillar.stem, 1.2)
            val boost = if (pillar.branch.wuXing == monthElement) 1.35 else 1.0
            addBranch(pillar.branch, boost)
        }
        scores[monthElement] = (scores[monthElement] ?: 0.0) + 0.8

        return FiveElementBalance(scores)
    }
}
