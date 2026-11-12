package com.gglee.baziyuce.engine

/** 以日主为基准的十神推导。 */
object TenGods {

    fun of(stem: HeavenlyStem, dayMaster: HeavenlyStem): TenGod {
        val me = dayMaster.wuXing
        val other = stem.wuXing
        val samePolarity = stem.yinYang == dayMaster.yinYang

        return when {
            other == me -> if (samePolarity) TenGod.BI_JIAN else TenGod.JIE_CAI
            other == me.generates -> if (samePolarity) TenGod.SHI_SHEN else TenGod.SHANG_GUAN
            other == me.controls -> if (samePolarity) TenGod.PIAN_CAI else TenGod.ZHENG_CAI
            other == me.controlledBy -> if (samePolarity) TenGod.QI_SHA else TenGod.ZHENG_GUAN
            else -> if (samePolarity) TenGod.PIAN_YIN else TenGod.ZHENG_YIN
        }
    }

    fun ofBranchMain(branch: EarthlyBranch, dayMaster: HeavenlyStem): TenGod =
        of(stem = branch.hiddenStems[0], dayMaster = dayMaster)
}
