package com.gglee.qimendunjia.engine

object InterpretationEngine {

    fun build(
        question: String,
        isYang: Boolean,
        ju: Int,
        zhiFu: NineStar,
        zhiShi: EightGate,
        zhiFuPalace: Palace,
        zhiShiPalace: Palace,
        cells: List<PalaceCell>,
        hour: StemBranch,
        xunKong: List<EarthlyBranch>,
        isYangDun: Boolean,
    ): List<InterpretationItem> {
        val q = question.trim()
        if (q.isEmpty()) {
            return listOf(
                InterpretationItem(
                    title = "请先填写所问之事",
                    detail = "起局页填写具体问题（如求财、出行、婚姻）后，解读会按用神宫位针对该问题展开。空问只能给空泛总论，意义有限。",
                    tone = InterpretationItem.Tone.CAUTION,
                ),
                InterpretationItem(
                    title = "局象（未绑定问题）",
                    detail = "${if (isYang) "阳遁" else "阴遁"}${ju}局。值符${zhiFu.hanzi}在${zhiFuPalace.hanzi}宫，值使${zhiShi.displayName}在${zhiShiPalace.hanzi}宫。请返回填写问题后重新排盘。",
                    tone = InterpretationItem.Tone.NEUTRAL,
                ),
            )
        }

        val topic = QuestionTopic.detect(q)
        val focus = YongShenMapping.focus(topic, cells)
        val focusCells = focus.palaces.mapNotNull { p -> cells.firstOrNull { it.palace == p } }

        val items = mutableListOf<InterpretationItem>()

        items.add(
            InterpretationItem(
                title = "所问之事",
                detail = "「$q」→ 归类为「${topic.rawValue}」。${topic.focusHint}。以下为规则模板，仅供学习参考，不作决策保证。",
                tone = InterpretationItem.Tone.NEUTRAL,
            ),
        )

        val palaceNotes = focusCells.take(4).map { cell ->
            describeCell(cell, isYangDun, focus)
        }.joinToString("\n")
        items.add(
            InterpretationItem(
                title = "与所问相关的宫位要点",
                detail = if (palaceNotes.isEmpty()) {
                    "未能定位明确用神宫，改看值符${zhiFuPalace.hanzi}与值使${zhiShiPalace.hanzi}。"
                } else {
                    palaceNotes
                },
                tone = InterpretationItem.Tone.NEUTRAL,
            ),
        )

        val (yi, shen, score) = yiShen(
            focus = focus,
            focusCells = focusCells,
            zhiFu = zhiFu,
            zhiShi = zhiShi,
            zhiFuPalace = zhiFuPalace,
            cells = cells,
        )
        items.add(InterpretationItem(title = "宜", detail = yi, tone = InterpretationItem.Tone.AUSPICIOUS))
        items.add(InterpretationItem(title = "慎", detail = shen, tone = InterpretationItem.Tone.CAUTION))

        items.add(
            InterpretationItem(
                title = "结合问题的一句话结论",
                detail = conclusion(q, topic, score, zhiShi, focusCells),
                tone = when {
                    score >= 1 -> InterpretationItem.Tone.AUSPICIOUS
                    score <= -1 -> InterpretationItem.Tone.CAUTION
                    else -> InterpretationItem.Tone.NEUTRAL
                },
            ),
        )

        items.add(
            InterpretationItem(
                title = "声明",
                detail = "解读由宫、门、星、神与空亡的启发式规则生成，非人工断验，亦非吉凶保证。重要用事请交叉历书并自行判断。",
                tone = InterpretationItem.Tone.NEUTRAL,
            ),
        )

        return items
    }

    private fun describeCell(cell: PalaceCell, isYangDun: Boolean, focus: YongShenFocus): String {
        val gate = cell.gate?.displayName ?: "—"
        val star = cell.star?.hanzi ?: "—"
        val deity = cell.deity?.name(isYangDun) ?: "—"
        val h = cell.heavenStem?.hanzi ?: "·"
        val e = cell.earthStem?.hanzi ?: "·"
        val marks = mutableListOf<String>()
        if (cell.isZhiFu) marks.add("值符")
        if (cell.isZhiShi) marks.add("值使")
        if (cell.isEmpty) marks.add("空亡")
        cell.gate?.let { if (focus.preferredGates.contains(it)) marks.add("事门") }
        cell.star?.let { if (focus.preferredStars.contains(it)) marks.add("事星") }
        val mark = if (marks.isEmpty()) "" else "〔${marks.joinToString("·")}〕"
        return "${cell.palace.hanzi}${cell.palace.rawValue}宫$mark：$star、$gate、$deity；天$h/地$e。"
    }

    private fun yiShen(
        focus: YongShenFocus,
        focusCells: List<PalaceCell>,
        zhiFu: NineStar,
        zhiShi: EightGate,
        zhiFuPalace: Palace,
        cells: List<PalaceCell>,
    ): Triple<String, String, Int> {
        var score = 0
        val yiParts = mutableListOf<String>()
        val shenParts = mutableListOf<String>()

        val goodGateHit = focusCells.any { cell ->
            val g = cell.gate ?: return@any false
            focus.preferredGates.contains(g) && g.isAuspicious && !cell.isEmpty
        }
        if (goodGateHit) {
            score += 1
            yiParts.add("事相关吉门落宫且未空，传统上利于推进所问。")
        }

        if (zhiShi.isAuspicious) {
            score += 1
            yiParts.add("值使${zhiShi.displayName}较利行动节奏。")
        } else {
            score -= 1
            shenParts.add("值使${zhiShi.displayName}偏滞，办事宜缓、留余地。")
        }

        if (zhiFu.isAuspicious) {
            score += 1
            yiParts.add("值符${zhiFu.hanzi}得力，主事人气场相对有助。")
        }

        if (focusCells.any { it.isEmpty }) {
            score -= 1
            shenParts.add("用神相关宫见空亡，事易虚实不定，勿过度承诺。")
        }

        val zfCell = cells.firstOrNull { it.palace == zhiFuPalace }
        if (zfCell?.star?.innatePalace == zhiFuPalace) {
            score -= 1
            shenParts.add("值符有伏吟倾向，所问或反复胶着，宜守不宜急。")
        }

        if (focus.topic == QuestionTopic.LAWSUIT) {
            if (focusCells.any { it.gate == EightGate.DU }) {
                yiParts.add("杜门临事宫，传统主宜守密、少张扬。")
            }
        }

        if (yiParts.isEmpty()) {
            yiParts.add("暂无明显「宜进」信号，可先观察用神宫变动与时机。")
        }
        if (shenParts.isEmpty()) {
            shenParts.add("未见强烈凶咎模板信号，仍须结合现实条件。")
        }

        return Triple(yiParts.joinToString(" "), shenParts.joinToString(" "), score)
    }

    private fun conclusion(
        question: String,
        topic: QuestionTopic,
        score: Int,
        zhiShi: EightGate,
        focusCells: List<PalaceCell>,
    ): String {
        val empty = focusCells.any { it.isEmpty }
        val tone = when {
            score >= 2 && !empty -> "就「$question」而言，盘面偏可尝试推进"
            score <= -1 || empty -> "就「$question」而言，盘面偏宜谨慎、缓图或改期"
            else -> "就「$question」而言，盘面中平，宜小步验证"
        }
        val gateNote = "（值使${zhiShi.displayName}，事项归类：${topic.rawValue}）"
        return tone + gateNote + "。请以现实筹划为准。"
    }
}
