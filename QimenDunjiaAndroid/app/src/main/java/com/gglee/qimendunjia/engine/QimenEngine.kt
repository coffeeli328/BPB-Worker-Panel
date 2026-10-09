package com.gglee.qimendunjia.engine

import java.util.Date
import java.util.TimeZone
import java.util.UUID

object QimenEngine {

    val xunShouYi: List<Pair<Int, HeavenlyStem>> = listOf(
        Pair(0, HeavenlyStem.WU),
        Pair(10, HeavenlyStem.JI),
        Pair(20, HeavenlyStem.GENG),
        Pair(30, HeavenlyStem.XIN),
        Pair(40, HeavenlyStem.REN),
        Pair(50, HeavenlyStem.GUI),
    )

    val qiYiOrder: List<HeavenlyStem> = listOf(
        HeavenlyStem.WU, HeavenlyStem.JI, HeavenlyStem.GENG, HeavenlyStem.XIN,
        HeavenlyStem.REN, HeavenlyStem.GUI, HeavenlyStem.DING, HeavenlyStem.BING, HeavenlyStem.YI,
    )

    data class PlateResult(
        val earth: Map<Palace, HeavenlyStem>,
        val heaven: Map<Palace, HeavenlyStem>,
        val stars: Map<Palace, NineStar>,
        val gates: Map<Palace, EightGate>,
        val deities: Map<Palace, EightDeity>,
        val zhiFuStar: NineStar,
        val zhiShiGate: EightGate,
        val zhiFuPalace: Palace,
        val zhiShiPalace: Palace,
        val xunKongBranches: List<EarthlyBranch>,
        val xunKongPalaces: Set<Palace>,
        val xunShouPalace: Palace,
        val yiStem: HeavenlyStem,
        val hourStemOnZhong: Boolean,
    )

    fun generate(request: ChartRequest): QimenChart {
        val tz = resolvedTimeZone(request)
        val civil = request.date

        val solarAdj = if (request.useTrueSolarTime) {
            TrueSolarTime.adjustedDate(
                civil = civil,
                timeZone = tz,
                longitudeEastDegrees = request.longitude,
                applyEquationOfTime = true,
            )
        } else {
            TrueSolarAdjustment(civil, 0.0, 0.0, 0.0)
        }

        val pillars = GanzhiCalendar.stemBranchFourPillars(forDate = solarAdj.date, timeZone = tz)
        val juRes = JuResolver.resolve(
            day = pillars.day,
            queryDate = civil,
            timeZone = tz,
            method = request.juMethod,
        )
        val termInstant = juRes.solarTermInstant
            ?: SolarTerms.currentTerm(forDate = civil, timeZone = tz).approximateDate

        val plate = buildPlate(
            isYangDun = juRes.isYangDun,
            juNumber = juRes.juNumber,
            hour = pillars.hour,
        )

        val cells = Palace.entries.map { p ->
            PalaceCell(
                palace = p,
                earthStem = plate.earth[p],
                heavenStem = plate.heaven[p],
                star = plate.stars[p],
                gate = plate.gates[p],
                deity = plate.deities[p],
                isEmpty = plate.xunKongPalaces.contains(p),
                isZhiFu = p == plate.zhiFuPalace,
                isZhiShi = p == plate.zhiShiPalace,
            )
        }

        val question = request.question.trim()
        val topic = QuestionTopic.detect(question)
        val interpretations = InterpretationEngine.build(
            question = question,
            isYang = juRes.isYangDun,
            ju = juRes.juNumber,
            zhiFu = plate.zhiFuStar,
            zhiShi = plate.zhiShiGate,
            zhiFuPalace = plate.zhiFuPalace,
            zhiShiPalace = plate.zhiShiPalace,
            cells = cells,
            hour = pillars.hour,
            xunKong = plate.xunKongBranches,
            isYangDun = juRes.isYangDun,
        )

        return QimenChart(
            id = UUID.randomUUID(),
            createdAt = Date(),
            queryDate = civil,
            trueSolarDate = solarAdj.date,
            calendarMode = request.calendarMode,
            timeZoneIdentifier = tz.id,
            method = request.method,
            locationNote = request.locationNote,
            longitude = request.longitude,
            usedTrueSolarTime = request.useTrueSolarTime,
            longitudeCorrectionMinutes = solarAdj.longitudeMinutes,
            equationOfTimeMinutes = solarAdj.eotMinutes,
            yearSB = pillars.year,
            monthSB = pillars.month,
            daySB = pillars.day,
            hourSB = pillars.hour,
            isYangDun = juRes.isYangDun,
            juNumber = juRes.juNumber,
            solarTermName = juRes.solarTermName,
            yuanName = juRes.yuanName,
            juMethod = juRes.juMethod,
            juPhase = juRes.phase,
            isRunQi = juRes.isRunQi,
            solarTermInstant = termInstant,
            zhiFuStar = plate.zhiFuStar,
            zhiShiGate = plate.zhiShiGate,
            zhiFuPalace = plate.zhiFuPalace,
            zhiShiPalace = plate.zhiShiPalace,
            xunKong = plate.xunKongBranches,
            cells = cells,
            question = question,
            questionTopic = topic,
            interpretations = interpretations,
        )
    }

    fun buildPlate(isYangDun: Boolean, juNumber: Int, hour: StemBranch): PlateResult {
        val earth = earthPlate(isYangDun = isYangDun, ju = juNumber)
        val (xunStart, yiStem) = xunShou(hour)
        val xunKong = xunKongBranches(xunStart)

        val xunShouPalace = earth.entries.firstOrNull { it.value == yiStem }?.key ?: Palace.KAN1
        val zhiFuStar = NineStar.from(xunShouPalace)
        val zhiShiGate = if (xunShouPalace == Palace.ZHONG5) {
            EightGate.SI
        } else {
            EightGate.innate(at = xunShouPalace) ?: EightGate.XIU
        }

        val hourStem = if (hour.stem == HeavenlyStem.JIA) yiStem else hour.stem
        var hourStemPalace = earth.entries.firstOrNull { it.value == hourStem }?.key ?: Palace.KAN1
        val hourStemOnZhong = hourStemPalace == Palace.ZHONG5
        if (hourStemOnZhong) hourStemPalace = Palace.ZHONG_HOST

        val zhiFuPalace = hourStemPalace
        val stars = rotateStars(zhiFu = zhiFuStar, to = zhiFuPalace)
        val heaven = rotateHeaven(earth = earth, stars = stars)

        val zhiShiRaw = moveZhiShiPalace(
            from = xunShouPalace,
            hourOffset = hour.sexagenaryIndex - xunStart,
            isYangDun = isYangDun,
        )
        val zhiShiDisplay = if (zhiShiRaw == Palace.ZHONG5) Palace.ZHONG_HOST else zhiShiRaw
        val gatesFinal = rotateGates(zhiShi = zhiShiGate, to = zhiShiDisplay)

        val deities = arrangeDeities(start = zhiFuPalace, isYangDun = isYangDun)
        val kongPalaces = xunKongPalaces(xunKong)

        return PlateResult(
            earth = earth,
            heaven = heaven,
            stars = stars,
            gates = gatesFinal,
            deities = deities,
            zhiFuStar = zhiFuStar,
            zhiShiGate = zhiShiGate,
            zhiFuPalace = zhiFuPalace,
            zhiShiPalace = zhiShiDisplay,
            xunKongBranches = xunKong,
            xunKongPalaces = kongPalaces,
            xunShouPalace = xunShouPalace,
            yiStem = yiStem,
            hourStemOnZhong = hourStemOnZhong,
        )
    }

    fun earthPlate(isYangDun: Boolean, ju: Int): Map<Palace, HeavenlyStem> {
        val map = mutableMapOf<Palace, HeavenlyStem>()
        val order = if (isYangDun) Palace.yangFly else Palace.yinFly
        val start = Palace.fromRaw(ju) ?: Palace.KAN1
        val sIdx = order.indexOf(start)
        if (sIdx < 0) return map
        qiYiOrder.forEachIndexed { i, stem ->
            map[order[(sIdx + i) % 9]] = stem
        }
        return map
    }

    fun xunShou(hour: StemBranch): Pair<Int, HeavenlyStem> {
        val idx = hour.sexagenaryIndex
        val start = (idx / 10) * 10
        val yi = xunShouYi.firstOrNull { it.first == start }?.second ?: HeavenlyStem.WU
        return Pair(start, yi)
    }

    fun xunKongBranches(xunStart: Int): List<EarthlyBranch> = when (xunStart) {
        0 -> listOf(EarthlyBranch.XU, EarthlyBranch.HAI)
        10 -> listOf(EarthlyBranch.SHEN, EarthlyBranch.YOU)
        20 -> listOf(EarthlyBranch.WU, EarthlyBranch.WEI)
        30 -> listOf(EarthlyBranch.CHEN, EarthlyBranch.SI)
        40 -> listOf(EarthlyBranch.YIN, EarthlyBranch.MAO)
        else -> listOf(EarthlyBranch.ZI, EarthlyBranch.CHOU)
    }

    fun xunKongPalaces(branches: List<EarthlyBranch>): Set<Palace> {
        val set = mutableSetOf<Palace>()
        val map = mapOf(
            EarthlyBranch.ZI to Palace.KAN1,
            EarthlyBranch.CHOU to Palace.GEN8,
            EarthlyBranch.YIN to Palace.GEN8,
            EarthlyBranch.MAO to Palace.ZHEN3,
            EarthlyBranch.CHEN to Palace.XUN4,
            EarthlyBranch.SI to Palace.XUN4,
            EarthlyBranch.WU to Palace.LI9,
            EarthlyBranch.WEI to Palace.KUN2,
            EarthlyBranch.SHEN to Palace.KUN2,
            EarthlyBranch.YOU to Palace.DUI7,
            EarthlyBranch.XU to Palace.QIAN6,
            EarthlyBranch.HAI to Palace.QIAN6,
        )
        for (b in branches) {
            map[b]?.let { set.add(it) }
        }
        return set
    }

    fun rotateStars(zhiFu: NineStar, to: Palace): Map<Palace, NineStar> {
        val ring = Palace.clockRing
        val starRing = NineStar.clockStars
        val pivotStar = if (zhiFu == NineStar.QIN) NineStar.RUI else zhiFu
        val toPalace = if (to == Palace.ZHONG5) Palace.ZHONG_HOST else to
        val fromIdx = starRing.indexOf(pivotStar)
        val toIdx = ring.indexOf(toPalace)
        if (fromIdx < 0 || toIdx < 0) return emptyMap()
        val shift = (toIdx - fromIdx + 8) % 8
        val map = mutableMapOf<Palace, NineStar>()
        for (i in 0 until 8) {
            map[ring[i]] = starRing[(i - shift + 8) % 8]
        }
        map[Palace.ZHONG5] = NineStar.QIN
        return map
    }

    fun rotateHeaven(earth: Map<Palace, HeavenlyStem>, stars: Map<Palace, NineStar>): Map<Palace, HeavenlyStem> {
        val heaven = mutableMapOf<Palace, HeavenlyStem>()
        for (src in Palace.entries) {
            val stem = earth[src] ?: continue
            val innate = NineStar.from(src)
            if (innate == NineStar.QIN) {
                val dest = stars.entries.firstOrNull { it.value == NineStar.RUI }?.key
                if (dest != null) heaven[dest] = stem
                continue
            }
            val dest = stars.entries.firstOrNull { it.value == innate }?.key
            if (dest != null) heaven[dest] = stem
        }
        if (heaven[Palace.ZHONG5] == null) earth[Palace.ZHONG5]?.let { heaven[Palace.ZHONG5] = it }
        for (p in Palace.entries) {
            if (heaven[p] == null) earth[p]?.let { heaven[p] = it }
        }
        return heaven
    }

    fun moveZhiShiPalace(from: Palace, hourOffset: Int, isYangDun: Boolean): Palace {
        val fly = if (isYangDun) Palace.yangFly else Palace.yinFly
        val sIdx = fly.indexOf(from)
        if (sIdx < 0) return from
        val steps = ((hourOffset % 9) + 9) % 9
        return fly[(sIdx + steps) % 9]
    }

    fun rotateGates(zhiShi: EightGate, to: Palace): Map<Palace, EightGate> {
        val ring = Palace.clockRing
        val gates = EightGate.clockGates
        val dest = if (to == Palace.ZHONG5) Palace.ZHONG_HOST else to
        val fromIdx = gates.indexOf(zhiShi)
        val toIdx = ring.indexOf(dest)
        if (fromIdx < 0 || toIdx < 0) return emptyMap()
        val shift = (toIdx - fromIdx + 8) % 8
        val map = mutableMapOf<Palace, EightGate>()
        for (i in 0 until 8) {
            map[ring[i]] = gates[(i - shift + 8) % 8]
        }
        return map
    }

    fun arrangeDeities(start: Palace, isYangDun: Boolean): Map<Palace, EightDeity> {
        val ring = Palace.clockRing
        val dest = if (start == Palace.ZHONG5) Palace.ZHONG_HOST else start
        val sIdx = ring.indexOf(dest)
        if (sIdx < 0) return emptyMap()
        val map = mutableMapOf<Palace, EightDeity>()
        val deities = EightDeity.entries
        for (i in 0 until 8) {
            val idx = if (isYangDun) (sIdx + i) % 8 else (sIdx - i + 8) % 8
            map[ring[idx]] = deities[i]
        }
        return map
    }

    private fun resolvedTimeZone(request: ChartRequest): TimeZone {
        val offset = request.timeZoneSecondsFromGMT
        if (offset != null) {
            val tz = TimeZone.getTimeZone(String.format("GMT%+d", offset / 3600))
            return if (tz.rawOffset == offset) tz else java.util.SimpleTimeZone(offset, "CustomGMT$offset")
        }
        return TimeZone.getDefault()
    }
}
