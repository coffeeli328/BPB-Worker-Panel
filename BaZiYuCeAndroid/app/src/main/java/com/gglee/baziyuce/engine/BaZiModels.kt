package com.gglee.baziyuce.engine

import java.util.Date
import java.util.UUID

enum class Gender(val displayName: String) {
    MALE("男"),
    FEMALE("女"),
}

enum class PillarKind(val displayName: String) {
    YEAR("年柱"),
    MONTH("月柱"),
    DAY("日柱"),
    HOUR("时柱"),
}

enum class TenGod(val displayName: String, val shortHint: String) {
    BI_JIAN("比肩", "同辈助力、自立进取"),
    JIE_CAI("劫财", "竞争分夺、行动果断"),
    SHI_SHEN("食神", "才情表达、轻松享乐"),
    SHANG_GUAN("伤官", "创意突破、口才锋芒"),
    PIAN_CAI("偏财", "偏财机遇、灵活经营"),
    ZHENG_CAI("正财", "正财稳收、务实积蓄"),
    QI_SHA("七杀", "压力挑战、果断决断"),
    ZHENG_GUAN("正官", "规矩责任、事业名声"),
    PIAN_YIN("偏印", "偏门学问、直觉灵感"),
    ZHENG_YIN("正印", "贵人学业、滋养庇护"),
}

data class PillarDetail(
    val kind: PillarKind,
    val stemBranch: StemBranch,
    val stemGod: TenGod?,
    val branchMainGod: TenGod?,
) {
    val name: String get() = stemBranch.name
}

data class FiveElementBalance(
    val scores: Map<WuXing, Double>,
) {
    fun score(wx: WuXing): Double = scores[wx] ?: 0.0

    val strongest: WuXing
        get() = WuXing.entries.maxByOrNull { score(it) } ?: WuXing.EARTH

    val weakest: WuXing
        get() = WuXing.entries.minByOrNull { score(it) } ?: WuXing.EARTH

    val summary: String
        get() = WuXing.entries.joinToString(" · ") { "${it.displayName}${score(it).toInt()}" }
}

data class BaZiChart(
    val id: String = UUID.randomUUID().toString(),
    val birthDate: Date,
    val gender: Gender,
    val timeZoneIdentifier: String,
    val year: StemBranch,
    val month: StemBranch,
    val day: StemBranch,
    val hour: StemBranch,
    val pillars: List<PillarDetail>,
    val balance: FiveElementBalance,
    val solarTermNote: String,
) {
    val dayMaster: HeavenlyStem get() = day.stem

    val fourPillarsText: String
        get() = "${year.name} ${month.name} ${day.name} ${hour.name}"
}

enum class FortunePeriod(val displayName: String, val sectionTitle: String) {
    YEAR("流年", "流年运势"),
    MONTH("流月", "流月运势"),
    DAY("流日", "流日运势"),
}

data class FortuneForecast(
    val period: FortunePeriod,
    val targetDate: Date,
    val flowingPillar: StemBranch,
    val stemGod: TenGod,
    val branchRelations: List<String>,
    val score: Int,
    val tone: String,
    val summary: String,
    val tips: List<String>,
    val ruleNotes: List<String>,
)

data class FourPillarsResult(
    val year: StemBranch,
    val month: StemBranch,
    val day: StemBranch,
    val hour: StemBranch,
    val note: String,
)
