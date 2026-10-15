package com.gglee.baziyuce.engine

enum class WuXing(val displayName: String) {
    WOOD("木"), FIRE("火"), EARTH("土"), METAL("金"), WATER("水");

    val generates: WuXing get() = entries[(ordinal + 1) % 5]
    val controls: WuXing get() = entries[(ordinal + 2) % 5]
    val generatedBy: WuXing get() = entries[(ordinal + 4) % 5]
    val controlledBy: WuXing get() = entries[(ordinal + 3) % 5]
}

enum class YinYang(val displayName: String) {
    YANG("阳"), YIN("阴");

    val opposite: YinYang get() = if (this == YANG) YIN else YANG
}

enum class HeavenlyStem(val displayName: String) {
    JIA("甲"), YI("乙"), BING("丙"), DING("丁"), WU("戊"),
    JI("己"), GENG("庚"), XIN("辛"), REN("壬"), GUI("癸");

    val yinYang: YinYang get() = if (ordinal % 2 == 0) YinYang.YANG else YinYang.YIN

    val wuXing: WuXing
        get() = when (this) {
            JIA, YI -> WuXing.WOOD
            BING, DING -> WuXing.FIRE
            WU, JI -> WuXing.EARTH
            GENG, XIN -> WuXing.METAL
            REN, GUI -> WuXing.WATER
        }

    companion object {
        fun from(index: Int): HeavenlyStem {
            val n = ((index % 10) + 10) % 10
            return entries[n]
        }
    }
}

enum class EarthlyBranch(val displayName: String) {
    ZI("子"), CHOU("丑"), YIN("寅"), MAO("卯"), CHEN("辰"), SI("巳"),
    WU("午"), WEI("未"), SHEN("申"), YOU("酉"), XU("戌"), HAI("亥");

    val yinYang: YinYang get() = if (ordinal % 2 == 0) YinYang.YANG else YinYang.YIN

    val wuXing: WuXing
        get() = when (this) {
            YIN, MAO -> WuXing.WOOD
            SI, WU -> WuXing.FIRE
            CHEN, XU, CHOU, WEI -> WuXing.EARTH
            SHEN, YOU -> WuXing.METAL
            HAI, ZI -> WuXing.WATER
        }

    /** 藏干（主气在前） */
    val hiddenStems: List<HeavenlyStem>
        get() = when (this) {
            ZI -> listOf(HeavenlyStem.GUI)
            CHOU -> listOf(HeavenlyStem.JI, HeavenlyStem.GUI, HeavenlyStem.XIN)
            YIN -> listOf(HeavenlyStem.JIA, HeavenlyStem.BING, HeavenlyStem.WU)
            MAO -> listOf(HeavenlyStem.YI)
            CHEN -> listOf(HeavenlyStem.WU, HeavenlyStem.YI, HeavenlyStem.GUI)
            SI -> listOf(HeavenlyStem.BING, HeavenlyStem.WU, HeavenlyStem.GENG)
            WU -> listOf(HeavenlyStem.DING, HeavenlyStem.JI)
            WEI -> listOf(HeavenlyStem.JI, HeavenlyStem.DING, HeavenlyStem.YI)
            SHEN -> listOf(HeavenlyStem.GENG, HeavenlyStem.REN, HeavenlyStem.WU)
            YOU -> listOf(HeavenlyStem.XIN)
            XU -> listOf(HeavenlyStem.WU, HeavenlyStem.XIN, HeavenlyStem.DING)
            HAI -> listOf(HeavenlyStem.REN, HeavenlyStem.JIA)
        }

    val clash: EarthlyBranch get() = from(ordinal + 6)

    val sixHarmony: EarthlyBranch
        get() {
            val map = mapOf(
                0 to 1, 1 to 0, 2 to 11, 3 to 10, 4 to 9, 5 to 8,
                6 to 7, 7 to 6, 8 to 5, 9 to 4, 10 to 3, 11 to 2,
            )
            return entries[map.getValue(ordinal)]
        }

    companion object {
        fun hourBranch(hour: Int): EarthlyBranch {
            val h = ((hour % 24) + 24) % 24
            val idx = ((h + 1) / 2) % 12
            return entries[idx]
        }

        fun from(index: Int): EarthlyBranch {
            val n = ((index % 12) + 12) % 12
            return entries[n]
        }
    }
}

data class StemBranch(
    val stem: HeavenlyStem,
    val branch: EarthlyBranch,
) {
    val name: String get() = stem.displayName + branch.displayName

    val sexagenaryIndex: Int
        get() {
            for (i in 0 until 60) {
                if (HeavenlyStem.from(i) == stem && EarthlyBranch.from(i) == branch) return i
            }
            return 0
        }

    companion object {
        fun from(sexagenaryIndex: Int): StemBranch {
            val n = ((sexagenaryIndex % 60) + 60) % 60
            return StemBranch(HeavenlyStem.from(n), EarthlyBranch.from(n))
        }

        fun parse(text: String): StemBranch? {
            if (text.length != 2) return null
            val stems = HeavenlyStem.entries.map { it.displayName }
            val branches = EarthlyBranch.entries.map { it.displayName }
            val sIdx = stems.indexOf(text.substring(0, 1))
            val bIdx = branches.indexOf(text.substring(1, 2))
            if (sIdx < 0 || bIdx < 0) return null
            return StemBranch(HeavenlyStem.entries[sIdx], EarthlyBranch.entries[bIdx])
        }
    }
}
