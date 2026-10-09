package com.gglee.qimendunjia.engine

enum class QuestionTopic(val rawValue: String) {
    WEALTH("求财"),
    TRAVEL("出行"),
    MARRIAGE("婚姻"),
    LAWSUIT("诉讼"),
    HEALTH("健康"),
    PARTNERSHIP("合作"),
    CAREER("求职"),
    LOST("寻物"),
    GENERAL("综合");

    val focusHint: String
        get() = when (this) {
            WEALTH -> "侧重生门、景门、开门及财气相关宫"
            TRAVEL -> "侧重开门、休门与值使落宫"
            MARRIAGE -> "侧重坤、兑宫及休门、生门"
            LAWSUIT -> "侧重杜门、惊门、伤门与值符"
            HEALTH -> "侧重天芮/死门相关宫，宜慎看空亡"
            PARTNERSHIP -> "侧重六合、开门、生门"
            CAREER -> "侧重开门、休门与值符值使"
            LOST -> "侧重生门、杜门与值使"
            GENERAL -> "以值符、值使宫为纲"
        }

    companion object {
        val quickTags: List<QuestionTopic> = listOf(
            WEALTH, TRAVEL, MARRIAGE, LAWSUIT, HEALTH, PARTNERSHIP, CAREER, LOST,
        )

        fun detect(from: String): QuestionTopic {
            val t = from.trim()
            if (t.isEmpty()) return GENERAL
            val rules = listOf(
                Pair(WEALTH, listOf("财", "钱", "生意", "投资", "收入", "买卖", "利润", "赚钱")),
                Pair(TRAVEL, listOf("出行", "旅游", "出差", "搬家", "远行", "行程", "出国")),
                Pair(MARRIAGE, listOf("婚姻", "恋爱", "感情", "桃花", "结婚", "复合", "分手")),
                Pair(LAWSUIT, listOf("诉讼", "官司", "纠纷", "起诉", "被告", "仲裁")),
                Pair(HEALTH, listOf("健康", "病", "身体", "就医", "手术", "康复")),
                Pair(PARTNERSHIP, listOf("合作", "合伙", "协议", "签约", "联营")),
                Pair(CAREER, listOf("工作", "求职", "升迁", "考试", "面试", "调动", "官运")),
                Pair(LOST, listOf("失物", "寻找", "丢失", "寻人", "找")),
            )
            for ((topic, keys) in rules) {
                if (keys.any { t.contains(it) }) return topic
            }
            return GENERAL
        }
    }
}

data class YongShenFocus(
    val topic: QuestionTopic,
    val palaces: List<Palace>,
    val preferredGates: List<EightGate>,
    val preferredStars: List<NineStar>,
    val preferredDeities: List<EightDeity>,
)

object YongShenMapping {

    fun focus(forTopic: QuestionTopic, cells: List<PalaceCell>): YongShenFocus {
        val gatePalaces: (EightGate) -> List<Palace> = { g ->
            cells.mapNotNull { if (it.gate == g) it.palace else null }
        }
        val deityPalaces: (EightDeity) -> List<Palace> = { d ->
            cells.mapNotNull { if (it.deity == d) it.palace else null }
        }

        return when (forTopic) {
            QuestionTopic.WEALTH -> {
                var p = gatePalaces(EightGate.SHENG) + gatePalaces(EightGate.JING) + gatePalaces(EightGate.KAI)
                p = p + listOf(Palace.KUN2, Palace.DUI7)
                YongShenFocus(
                    forTopic,
                    unique(p),
                    listOf(EightGate.SHENG, EightGate.JING, EightGate.KAI),
                    listOf(NineStar.XIN, NineStar.FU, NineStar.REN),
                    listOf(EightDeity.LIU_HE, EightDeity.JIU_TIAN),
                )
            }
            QuestionTopic.TRAVEL -> YongShenFocus(
                forTopic,
                unique(gatePalaces(EightGate.KAI) + gatePalaces(EightGate.XIU) + cells.filter { it.isZhiShi }.map { it.palace }),
                listOf(EightGate.KAI, EightGate.XIU),
                listOf(NineStar.CHONG, NineStar.FU),
                listOf(EightDeity.JIU_TIAN),
            )
            QuestionTopic.MARRIAGE -> YongShenFocus(
                forTopic,
                unique(listOf(Palace.KUN2, Palace.DUI7) + gatePalaces(EightGate.XIU) + gatePalaces(EightGate.SHENG)),
                listOf(EightGate.XIU, EightGate.SHENG),
                listOf(NineStar.REN, NineStar.XIN),
                listOf(EightDeity.TAI_YIN, EightDeity.LIU_HE),
            )
            QuestionTopic.LAWSUIT -> YongShenFocus(
                forTopic,
                unique(
                    gatePalaces(EightGate.DU) + gatePalaces(EightGate.JING_SHOCK) +
                        gatePalaces(EightGate.SHANG) + cells.filter { it.isZhiFu }.map { it.palace },
                ),
                listOf(EightGate.DU, EightGate.JING_SHOCK, EightGate.SHANG),
                listOf(NineStar.ZHU, NineStar.PENG),
                listOf(EightDeity.BAI_HU, EightDeity.XUAN_WU),
            )
            QuestionTopic.HEALTH -> YongShenFocus(
                forTopic,
                unique(
                    gatePalaces(EightGate.SI) +
                        cells.mapNotNull { if (it.star == NineStar.RUI || it.star == NineStar.QIN) it.palace else null } +
                        listOf(Palace.KUN2, Palace.ZHONG5),
                ),
                listOf(EightGate.SI, EightGate.JING_SHOCK),
                listOf(NineStar.RUI, NineStar.QIN),
                listOf(EightDeity.TENG_SHE),
            )
            QuestionTopic.PARTNERSHIP -> YongShenFocus(
                forTopic,
                unique(deityPalaces(EightDeity.LIU_HE) + gatePalaces(EightGate.KAI) + gatePalaces(EightGate.SHENG)),
                listOf(EightGate.KAI, EightGate.SHENG),
                listOf(NineStar.FU, NineStar.XIN),
                listOf(EightDeity.LIU_HE),
            )
            QuestionTopic.CAREER -> YongShenFocus(
                forTopic,
                unique(
                    gatePalaces(EightGate.KAI) + gatePalaces(EightGate.XIU) +
                        cells.filter { it.isZhiFu || it.isZhiShi }.map { it.palace },
                ),
                listOf(EightGate.KAI, EightGate.XIU),
                listOf(NineStar.XIN, NineStar.FU),
                listOf(EightDeity.JIU_TIAN, EightDeity.ZHI_FU),
            )
            QuestionTopic.LOST -> YongShenFocus(
                forTopic,
                unique(gatePalaces(EightGate.SHENG) + gatePalaces(EightGate.DU) + cells.filter { it.isZhiShi }.map { it.palace }),
                listOf(EightGate.SHENG, EightGate.DU),
                listOf(NineStar.REN),
                listOf(EightDeity.LIU_HE),
            )
            QuestionTopic.GENERAL -> YongShenFocus(
                forTopic,
                unique(cells.filter { it.isZhiFu || it.isZhiShi }.map { it.palace }),
                emptyList(),
                emptyList(),
                listOf(EightDeity.ZHI_FU),
            )
        }
    }

    private fun unique(palaces: List<Palace>): List<Palace> {
        val seen = mutableSetOf<Palace>()
        return palaces.filter { seen.add(it) }
    }
}
