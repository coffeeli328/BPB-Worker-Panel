package com.gglee.ziweidoushu.engine

object SiHuaResolver {

    /**
     * 生年（及大限/流年天干）四化表。
     * 甲廉破武阳、乙机梁紫阴、丙同机昌廉、丁阴同机巨、
     * 戊贪阴右机、己武贪梁曲、庚阳武阴同、辛巨阳曲昌、
     * 壬梁紫左武、癸破巨阴贪。
     */
    fun starsFor(stem: HeavenlyStem): Map<SiHua, Star> = when (stem) {
        HeavenlyStem.JIA -> mapOf(
            SiHua.LU to Star.LIAN_ZHEN,
            SiHua.QUAN to Star.PO_JUN,
            SiHua.KE to Star.WU_QU,
            SiHua.JI to Star.TAI_YANG,
        )
        HeavenlyStem.YI -> mapOf(
            SiHua.LU to Star.TIAN_JI,
            SiHua.QUAN to Star.TIAN_LIANG,
            SiHua.KE to Star.ZI_WEI,
            SiHua.JI to Star.TAI_YIN,
        )
        HeavenlyStem.BING -> mapOf(
            SiHua.LU to Star.TIAN_TONG,
            SiHua.QUAN to Star.TIAN_JI,
            SiHua.KE to Star.WEN_CHANG,
            SiHua.JI to Star.LIAN_ZHEN,
        )
        HeavenlyStem.DING -> mapOf(
            SiHua.LU to Star.TAI_YIN,
            SiHua.QUAN to Star.TIAN_TONG,
            SiHua.KE to Star.TIAN_JI,
            SiHua.JI to Star.JU_MEN,
        )
        HeavenlyStem.WU -> mapOf(
            SiHua.LU to Star.TAN_LANG,
            SiHua.QUAN to Star.TAI_YIN,
            SiHua.KE to Star.YOU_BI,
            SiHua.JI to Star.TIAN_JI,
        )
        HeavenlyStem.JI -> mapOf(
            SiHua.LU to Star.WU_QU,
            SiHua.QUAN to Star.TAN_LANG,
            SiHua.KE to Star.TIAN_LIANG,
            SiHua.JI to Star.WEN_QU,
        )
        HeavenlyStem.GENG -> mapOf(
            SiHua.LU to Star.TAI_YANG,
            SiHua.QUAN to Star.WU_QU,
            SiHua.KE to Star.TAI_YIN,
            SiHua.JI to Star.TIAN_TONG,
        )
        HeavenlyStem.XIN -> mapOf(
            SiHua.LU to Star.JU_MEN,
            SiHua.QUAN to Star.TAI_YANG,
            SiHua.KE to Star.WEN_QU,
            SiHua.JI to Star.WEN_CHANG,
        )
        HeavenlyStem.REN -> mapOf(
            SiHua.LU to Star.TIAN_LIANG,
            SiHua.QUAN to Star.ZI_WEI,
            SiHua.KE to Star.ZUO_FU,
            SiHua.JI to Star.WU_QU,
        )
        HeavenlyStem.GUI -> mapOf(
            SiHua.LU to Star.PO_JUN,
            SiHua.QUAN to Star.JU_MEN,
            SiHua.KE to Star.TAI_YIN,
            SiHua.JI to Star.TAN_LANG,
        )
    }

    fun resolve(
        stem: HeavenlyStem,
        cells: List<PalaceCell>,
    ): List<SiHuaEntry> {
        val starBranch = mutableMapOf<Star, Pair<EarthlyBranch, PalaceType>>()
        cells.forEach { cell ->
            cell.stars.forEach { ps ->
                starBranch[ps.star] = cell.branch to cell.type
            }
        }
        return SiHua.entries.mapNotNull { sh ->
            val star = starsFor(stem)[sh] ?: return@mapNotNull null
            val loc = starBranch[star] ?: return@mapNotNull null
            SiHuaEntry(sh, star, loc.first, loc.second)
        }
    }

    fun natalMarkMap(stem: HeavenlyStem): Map<Star, SiHua> =
        starsFor(stem).entries.associate { (sh, star) -> star to sh }
}
