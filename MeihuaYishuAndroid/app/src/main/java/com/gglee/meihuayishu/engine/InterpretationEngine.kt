package com.gglee.meihuayishu.engine

object InterpretationEngine {

    fun interpret(chart: MeihuaChart): List<InterpretationItem> {
        val items = mutableListOf<InterpretationItem>()
        items += InterpretationItem(
            title = "体用总断",
            detail = buildString {
                append("体卦${chart.ti.displayName}（${chart.ti.element.hanzi}）为事之主体；")
                append("用卦${chart.yong.displayName}（${chart.yong.element.hanzi}）为所问之人/事。")
                append("二者关系：${chart.tiYongRelation.label}——${chart.tiYongRelation.hint}。")
                if (chart.hasQuestion) {
                    append("针对「${chart.question}」，先观体用生克，再参互变。")
                }
            },
            tone = when (chart.tiYongRelation) {
                TiYongRelation.YONG_SHENG_TI, TiYongRelation.BI_HE -> Tone.POSITIVE
                TiYongRelation.TI_KE_YONG -> Tone.NEUTRAL
                TiYongRelation.TI_SHENG_YONG, TiYongRelation.YONG_KE_TI -> Tone.CAUTION
            },
        )

        items += InterpretationItem(
            title = "本卦 · ${chart.original.name}",
            detail = "上${chart.original.upper.displayName}下${chart.original.lower.displayName}。" +
                "动爻在第${chart.movingLine}爻（自下而上）。" +
                lineHint(chart.movingLine) +
                "本卦示事之初始、当前情势。",
            tone = Tone.NEUTRAL,
        )

        items += InterpretationItem(
            title = "互卦 · ${chart.mutual.name}",
            detail = "互上${chart.mutual.upper.displayName}、互下${chart.mutual.lower.displayName}，" +
                "多主事情发展的中间过程与隐藏因素。可与体用对照：互中见转机或伏笔。",
            tone = Tone.NEUTRAL,
        )

        items += InterpretationItem(
            title = "变卦 · ${chart.changed.name}",
            detail = "动爻变化后上${chart.changed.upper.displayName}下${chart.changed.lower.displayName}，" +
                "多主事之结局、未来趋向。变卦体用若与本卦生克不同，当以变为归宿参详。",
            tone = Tone.NEUTRAL,
        )

        items += InterpretationItem(
            title = "五行提示",
            detail = wuXingAdvice(chart),
            tone = when (chart.tiYongRelation) {
                TiYongRelation.YONG_SHENG_TI -> Tone.POSITIVE
                TiYongRelation.YONG_KE_TI -> Tone.CAUTION
                else -> Tone.NEUTRAL
            },
        )

        items += InterpretationItem(
            title = "学习说明",
            detail = "以上为梅花易数体用生克之规则纲要，仅供学习参考，不作绝对吉凶断言，" +
                "亦非医疗、法律或投资建议。可将本盘导出图片交给其他 AI 作进一步文本解读。",
            tone = Tone.NEUTRAL,
        )
        return items
    }

    private fun lineHint(moving: Int): String = when (moving) {
        1 -> "初爻动，事之发端，宜慎始。"
        2 -> "二爻动，多涉人情、协作。"
        3 -> "三爻动，多有进退、选择。"
        4 -> "四爻动，近于外事、公门或他人。"
        5 -> "五爻动，多主尊长、核心决策。"
        else -> "上爻动，事将尽、结果将显。"
    }

    private fun wuXingAdvice(chart: MeihuaChart): String {
        val ti = chart.ti.element
        val yong = chart.yong.element
        return when (chart.tiYongRelation) {
            TiYongRelation.BI_HE ->
                "体用皆属${ti.hanzi}，气势相当。宜稳健推进，忌妄动生变。"
            TiYongRelation.YONG_SHENG_TI ->
                "用${yong.hanzi}生体${ti.hanzi}，外力来助。可借势、合作，但勿贪多。"
            TiYongRelation.TI_SHENG_YONG ->
                "体${ti.hanzi}生用${yong.hanzi}，自身有耗。宜量力、设边界，防过度付出。"
            TiYongRelation.TI_KE_YONG ->
                "体${ti.hanzi}克用${yong.hanzi}，主动有余。可成事而须防对方反弹、阻力。"
            TiYongRelation.YONG_KE_TI ->
                "用${yong.hanzi}克体${ti.hanzi}，外势压己。宜避锋、缓进，先护体再图用。"
        }
    }
}
