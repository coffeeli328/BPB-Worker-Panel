package com.gglee.ziweidoushu.engine

object InterpretationEngine {

    private val starHints = mapOf(
        Star.ZI_WEI to "紫微为北斗帝星，入庙主贵气领导，宜掌权统筹；落陷则志大才疏，需借辅弼成事。",
        Star.TIAN_JI to "天机主谋略善变，利策划学习；静则生慧，躁则多忧。",
        Star.TAI_YANG to "太阳主光明赏识，日生人更显；照官禄利于名，入迁移利外出发展。",
        Star.WU_QU to "武曲主财帛刚毅，宜金融技术实干；过刚则孤，需软辅调和。",
        Star.TIAN_TONG to "天同主福气随和，少忧少争；进取心弱时宜自驱，喜见禄马。",
        Star.LIAN_ZHEN to "廉贞主二次人际与是非，可化二次桃花或官非；宜正直自守。",
        Star.TIAN_FU to "天府主库藏稳厚，守成有余；喜见财星，忌空劫耗财。",
        Star.TAI_YIN to "太阴主静默细腻与田宅，夜生人更显；利艺术财务内务。",
        Star.TAN_LANG to "贪狼主欲望才艺桃花，善交际拓展；节制则成，放纵则耗。",
        Star.JU_MEN to "巨门主口舌暗动，利研究口才法律；慎言可免是非。",
        Star.TIAN_XIANG to "天相主印绶辅助，善协调文书；宜公职幕僚，忌过柔失断。",
        Star.TIAN_LIANG to "天梁主清贵荫庇，逢难有解；利医疗教育监察。",
        Star.QI_SHA to "七杀主权谋冲决，宜开创专才；需有化忌化解之星同宫方稳。",
        Star.PO_JUN to "破军主破立革新，先破后成；利改革军警技术突破。",
        Star.ZUO_FU to "左辅为助星，同宫增贵，主贵人提携。",
        Star.YOU_BI to "右弼为助星，与左辅齐力，利合作人缘。",
        Star.WEN_CHANG to "文昌主考试文书功名，利科甲笔墨。",
        Star.WEN_QU to "文曲主才艺灵巧，利艺术表达与偏财机缘。",
        Star.TIAN_KUI to "天魁为阳贵人，主白天贵人提携。",
        Star.TIAN_YUE to "天钺为阴贵人，主暗中贵人照应。",
        Star.LU_CUN to "禄存主财禄稳收，守财有方；同宫可解部分煞气。",
        Star.TIAN_MA to "天马主动迁变动，利外出发展与流动性财。",
        Star.QING_YANG to "擎羊为刑煞，主刚烈挫折；宜武职手术技术，忌冲动。",
        Star.TUO_LUO to "陀罗为忌滞，主拖延纠缠；宜耐心磨练专精。",
        Star.HUO_XING to "火星主急躁爆发，速成亦速变；宜爆发力行业。",
        Star.LING_XING to "铃星主阴煞尖锐，心思细密亦孤克；宜专研独处。",
        Star.DI_KONG to "地空主空虚理想，破财亦破执；宜宗教哲学艺术。",
        Star.DI_JIE to "地劫主波折变动，劳心费力；宜流动竞争之业。",
    )

    private val palaceHints = mapOf(
        PalaceType.MING to "命宫看格局性情与人生主轴。",
        PalaceType.XIONG_DI to "兄弟宫看手足同事同辈关系。",
        PalaceType.FU_QI to "夫妻宫看婚姻伴侣与合作关系。",
        PalaceType.ZI_NV to "子女宫看子女缘与部属晚辈。",
        PalaceType.CAI_BO to "财帛宫看求财方式与理财态度。",
        PalaceType.JI_E to "疾厄宫看体质劳损与压力来源。",
        PalaceType.QIAN_YI to "迁移宫看外出环境与社会舞台。",
        PalaceType.JIAO_YOU to "交友宫看朋友部属与社交圈。",
        PalaceType.GUAN_LU to "官禄宫看事业志向与社会角色。",
        PalaceType.TIAN_ZHAI to "田宅宫看不动产家居与内心归宿。",
        PalaceType.FU_DE to "福德宫看精神享受与兴趣心态。",
        PalaceType.FU_MU to "父母宫看长辈缘与学习文书贵人。",
    )

    fun interpret(chart: ZiWeiChart): List<InterpretationItem> {
        val items = mutableListOf<InterpretationItem>()
        val ming = chart.cell(PalaceType.MING)
        val majors = ming.majorStars

        items += InterpretationItem(
            title = "命盘总览",
            detail = buildString {
                append("命宫在${ming.stem.hanzi}${ming.branch.hanzi}，")
                append(chart.fiveElementBureau.displayName)
                append("，身宫在${chart.shenBranch.hanzi}。")
                if (majors.isEmpty()) {
                    append("命宫无主星，需借对宫星曜论性情格局。")
                } else {
                    append("命宫主星：")
                    append(majors.joinToString("、") { "${it.star.hanzi}（${it.brightness.hanzi}）" })
                    append("。")
                }
                append("紫微落${chart.ziweiBranch.hanzi}。大限起运约${chart.fiveElementBureau.number}岁。")
            },
            tone = InterpretationTone.NEUTRAL,
        )

        majors.forEach { placed ->
            starHints[placed.star]?.let { hint ->
                items += InterpretationItem(
                    title = "命宫 · ${placed.star.hanzi}",
                    detail = hint,
                    tone = when (placed.brightness) {
                        Brightness.MIAO, Brightness.WANG -> InterpretationTone.POSITIVE
                        Brightness.XIAN, Brightness.BU_LI -> InterpretationTone.CAUTION
                        else -> InterpretationTone.NEUTRAL
                    },
                )
            }
        }

        listOf(PalaceType.CAI_BO, PalaceType.GUAN_LU, PalaceType.FU_QI, PalaceType.QIAN_YI).forEach { type ->
            val cell = chart.cell(type)
            val stars = cell.majorStars
            val hint = palaceHints[type].orEmpty()
            val starLine = if (stars.isEmpty()) {
                "无主星，借对宫观察。"
            } else {
                "主星：" + stars.joinToString("、") { "${it.star.hanzi}${it.brightness.hanzi}" } + "。"
            }
            val aux = cell.otherStars.take(4).joinToString("、") { it.star.hanzi }
            items += InterpretationItem(
                title = type.hanzi,
                detail = buildString {
                    append(hint)
                    append(starLine)
                    if (aux.isNotEmpty()) append("同宫见$aux。")
                    append("大限约${cell.decadeAgeStart}–${cell.decadeAgeEnd}岁行至此宫。")
                },
                tone = toneFor(stars),
            )
        }

        val sha = ming.stars.filter {
            it.star in setOf(Star.QING_YANG, Star.TUO_LUO, Star.HUO_XING, Star.LING_XING, Star.DI_KONG, Star.DI_JIE)
        }
        if (sha.isNotEmpty()) {
            items += InterpretationItem(
                title = "命宫煞曜提醒",
                detail = "命宫见" + sha.joinToString("、") { it.star.hanzi } +
                    "。煞不忌有用，宜化作专精动力，避免冲动争执与无谓损耗。",
                tone = InterpretationTone.CAUTION,
            )
        }

        items += InterpretationItem(
            title = "使用说明",
            detail = "本解读为三合派安星规则下的学习参考摘要，不构成命运断言，亦不涉及医疗、法律或投资建议。完整论断需结合四化、大限流年与实际经历。",
            tone = InterpretationTone.NEUTRAL,
        )
        return items
    }

    private fun toneFor(stars: List<PlacedStar>): InterpretationTone {
        if (stars.any { it.brightness == Brightness.MIAO || it.brightness == Brightness.WANG }) {
            return InterpretationTone.POSITIVE
        }
        if (stars.any { it.brightness == Brightness.XIAN }) return InterpretationTone.CAUTION
        return InterpretationTone.NEUTRAL
    }
}
