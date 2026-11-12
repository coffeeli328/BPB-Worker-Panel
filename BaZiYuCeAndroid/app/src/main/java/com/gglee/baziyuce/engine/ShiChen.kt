package com.gglee.baziyuce.engine

/**
 * 十二时辰（与 iOS BaZiSession.ShiChen 同口径）。
 * 子时代表钟点为 23，便于子时换日。
 */
data class ShiChen(
    val branch: EarthlyBranch,
    val representativeHour: Int,
) {
    val displayName: String get() = "${branch.displayName}时（$timeRangeLabel）"

    val timeRangeLabel: String
        get() = when (branch) {
            EarthlyBranch.ZI -> "23–1点"
            EarthlyBranch.CHOU -> "1–3点"
            EarthlyBranch.YIN -> "3–5点"
            EarthlyBranch.MAO -> "5–7点"
            EarthlyBranch.CHEN -> "7–9点"
            EarthlyBranch.SI -> "9–11点"
            EarthlyBranch.WU -> "11–13点"
            EarthlyBranch.WEI -> "13–15点"
            EarthlyBranch.SHEN -> "15–17点"
            EarthlyBranch.YOU -> "17–19点"
            EarthlyBranch.XU -> "19–21点"
            EarthlyBranch.HAI -> "21–23点"
        }

    companion object {
        val all: List<ShiChen> = EarthlyBranch.entries.map { branch ->
            val hour = when (branch) {
                EarthlyBranch.ZI -> 23
                EarthlyBranch.CHOU -> 1
                EarthlyBranch.YIN -> 3
                EarthlyBranch.MAO -> 5
                EarthlyBranch.CHEN -> 7
                EarthlyBranch.SI -> 9
                EarthlyBranch.WU -> 11
                EarthlyBranch.WEI -> 13
                EarthlyBranch.SHEN -> 15
                EarthlyBranch.YOU -> 17
                EarthlyBranch.XU -> 19
                EarthlyBranch.HAI -> 21
            }
            ShiChen(branch = branch, representativeHour = hour)
        }
    }
}
