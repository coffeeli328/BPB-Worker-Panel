package com.gglee.baziyuce.engine

/** 地支刑冲合害简表（规则透明、可扩展）。 */
object BranchRelations {

    fun describe(flowing: EarthlyBranch, natal: List<EarthlyBranch>): List<String> {
        val notes = mutableListOf<String>()
        for (b in natal) {
            if (flowing.clash == b) {
                notes += "${flowing.displayName}冲${b.displayName}"
            }
            if (flowing.sixHarmony == b) {
                notes += "${flowing.displayName}合${b.displayName}"
            }
            if (isHarm(flowing, b)) {
                notes += "${flowing.displayName}害${b.displayName}"
            }
            if (isXing(flowing, b)) {
                notes += "${flowing.displayName}刑${b.displayName}"
            }
        }
        return notes.distinct()
    }

    private fun isHarm(a: EarthlyBranch, b: EarthlyBranch): Boolean {
        val pairs = setOf(
            setOf(0, 7), setOf(1, 6), setOf(2, 5),
            setOf(3, 4), setOf(8, 11), setOf(9, 10),
        )
        return setOf(a.ordinal, b.ordinal) in pairs
    }

    private fun isXing(a: EarthlyBranch, b: EarthlyBranch): Boolean {
        if (a == b) {
            return a.ordinal in listOf(4, 6, 9, 11)
        }
        val triples = listOf(
            listOf(2, 5, 8),
            listOf(1, 7, 10),
            listOf(0, 3),
        )
        return triples.any { t -> a.ordinal in t && b.ordinal in t }
    }
}
