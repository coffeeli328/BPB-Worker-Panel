package com.gglee.qimendunjia.engine

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.abs

data class ZhiYunYuanSlot(
    val termName: String,
    val yuanIndex: Int,
    val start: Date,
    val end: Date,
    val isRunQi: Boolean,
    val termInstant: Date,
)

object ZhiYunResolver {

    fun isShangYuanFuTou(sb: StemBranch): Boolean {
        if (sb.stem != HeavenlyStem.JIA && sb.stem != HeavenlyStem.JI) return false
        return when (sb.branch) {
            EarthlyBranch.ZI, EarthlyBranch.WU, EarthlyBranch.MAO, EarthlyBranch.YOU -> true
            else -> false
        }
    }

    fun resolve(queryDate: Date, day: StemBranch, timeZone: TimeZone): JuResolution {
        val slots = buildSchedule(around = queryDate, timeZone = timeZone)
        val dayStart = startOfDay(queryDate, timeZone)
        val slot = slots.lastOrNull { it.start <= dayStart && dayStart < it.end }
            ?: slots.firstOrNull { it.start <= dayStart }
            ?: slots.lastOrNull()

        if (slot == null) {
            val term = SolarTerms.currentTerm(forDate = queryDate, timeZone = timeZone)
            val r = JuResolver.resolveChaibu(day = day, solarTermName = term.name)
            return r.copy(
                juMethod = JuMethod.ZHI_YUN,
                phase = ZhiYunPhase.CHAO_SHEN,
                isRunQi = false,
                solarTermInstant = term.approximateDate,
            )
        }

        val ft = JuResolver.fuTou(day)
        val entry = JuResolver.termJuEntry(slot.termName)
        val ju = entry.second[slot.yuanIndex]
        val phase = classifyPhase(
            shangStart = slotsShangStart(forSlot = slot, inSlots = slots),
            termInstant = slot.termInstant,
            isRunQi = slot.isRunQi,
        )
        return JuResolution(
            isYangDun = entry.first,
            juNumber = ju,
            yuanName = JuResolver.yuanName(slot.yuanIndex),
            yuanIndex = slot.yuanIndex,
            solarTermName = slot.termName + if (slot.isRunQi) "（闰）" else "",
            fuTou = ft,
            juMethod = JuMethod.ZHI_YUN,
            phase = phase,
            isRunQi = slot.isRunQi,
            solarTermInstant = slot.termInstant,
        )
    }

    fun buildSchedule(around: Date, timeZone: TimeZone): List<ZhiYunYuanSlot> {
        val cal = Calendar.getInstance(timeZone)
        cal.time = around
        val year = cal.get(Calendar.YEAR)

        val terms = mutableListOf<SolarTermInfo>()
        for (y in (year - 1)..(year + 1)) {
            terms.addAll(SolarTerms.terms(forSolarYear = y))
        }
        terms.sortBy { it.approximateDate }

        val rangeStart = calendarDate(year - 1, 1, 1, timeZone)
        val rangeEnd = calendarDate(year + 2, 1, 1, timeZone)
        val yuanHeads = allJiaJiDays(from = rangeStart, to = rangeEnd, timeZone = timeZone)

        val dongZhiIdx = terms.indexOfFirst { it.name == "冬至" }
        if (dongZhiIdx < 0) return emptyList()
        val t0 = terms[dongZhiIdx]
        val shangHeads = yuanHeads.filter { isShangYuanFuTou(dayStemBranch(on = it, timeZone = timeZone)) }

        var yuanHeadIdx = indexOfAlignedShangFuTou(
            termInstant = t0.approximateDate,
            shangHeads = shangHeads,
            timeZone = timeZone,
        ) ?: return emptyList()

        fun yuanHeadsIndex(ofShang: Int): Int? {
            val d = shangHeads[ofShang]
            return yuanHeads.indexOf(d)
        }

        var cursor = yuanHeadsIndex(yuanHeadIdx) ?: return emptyList()

        val slots = mutableListOf<ZhiYunYuanSlot>()
        for (tIdx in dongZhiIdx until terms.size) {
            val term = terms[tIdx]
            if (cursor + 2 >= yuanHeads.size) break

            val shangStart = yuanHeads[cursor]
            for (y in 0..2) {
                val start = yuanHeads[cursor + y]
                val end = if (cursor + y + 1 < yuanHeads.size) {
                    yuanHeads[cursor + y + 1]
                } else {
                    Date(start.time + 5 * 86400000L)
                }
                slots.add(
                    ZhiYunYuanSlot(
                        termName = term.name,
                        yuanIndex = y,
                        start = startOfDay(start, timeZone),
                        end = startOfDay(end, timeZone),
                        isRunQi = false,
                        termInstant = term.approximateDate,
                    ),
                )
            }
            cursor += 3

            if (term.name == "芒种" || term.name == "大雪") {
                val chaoDays = calendarDays(
                    from = startOfDay(shangStart, timeZone),
                    to = term.approximateDate,
                    timeZone = timeZone,
                )
                if (shangStart <= term.approximateDate && chaoDays >= 9) {
                    if (cursor + 2 >= yuanHeads.size) break
                    for (y in 0..2) {
                        val start = yuanHeads[cursor + y]
                        val end = if (cursor + y + 1 < yuanHeads.size) {
                            yuanHeads[cursor + y + 1]
                        } else {
                            Date(start.time + 5 * 86400000L)
                        }
                        slots.add(
                            ZhiYunYuanSlot(
                                termName = term.name,
                                yuanIndex = y,
                                start = startOfDay(start, timeZone),
                                end = startOfDay(end, timeZone),
                                isRunQi = true,
                                termInstant = term.approximateDate,
                            ),
                        )
                    }
                    cursor += 3
                }
            }
        }
        return slots
    }

    fun indexOfAlignedShangFuTou(
        termInstant: Date,
        shangHeads: List<Date>,
        timeZone: TimeZone,
    ): Int? {
        val termDay = startOfDay(termInstant, timeZone)
        var bestBefore: Int? = null
        for ((i, d) in shangHeads.withIndex()) {
            val sd = startOfDay(d, timeZone)
            if (sd <= termDay) bestBefore = i
            else break
        }
        if (bestBefore != null) return bestBefore
        return shangHeads.indexOfFirst { startOfDay(it, timeZone) > termDay }.takeIf { it >= 0 }
    }

    private fun slotsShangStart(forSlot: ZhiYunYuanSlot, inSlots: List<ZhiYunYuanSlot>): Date {
        if (forSlot.yuanIndex == 0) return forSlot.start
        val idx = inSlots.indexOfFirst { it.start == forSlot.start }
        if (idx >= 0) {
            val shangIdx = idx - forSlot.yuanIndex
            if (shangIdx >= 0) return inSlots[shangIdx].start
        }
        return forSlot.start
    }

    private fun classifyPhase(shangStart: Date, termInstant: Date, isRunQi: Boolean): ZhiYunPhase {
        if (isRunQi) return ZhiYunPhase.RUN_QI
        val s = shangStart.time / 1000.0
        val t = termInstant.time / 1000.0
        val dayDiff = abs(s - t) / 86400.0
        if (dayDiff < 1.0) return ZhiYunPhase.ZHENG_SHOU
        if (s < t) return ZhiYunPhase.CHAO_SHEN
        return ZhiYunPhase.JIE_QI
    }

    fun startOfDay(date: Date, timeZone: TimeZone): Date {
        val cal = Calendar.getInstance(timeZone)
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    fun calendarDays(from: Date, to: Date, timeZone: TimeZone): Int {
        val cal = Calendar.getInstance(timeZone)
        val start = startOfDay(from, timeZone)
        val end = startOfDay(to, timeZone)
        val msPerDay = 86400000L
        return ((end.time - start.time) / msPerDay).toInt()
    }

    fun allJiaJiDays(from: Date, to: Date, timeZone: TimeZone): List<Date> {
        val cal = Calendar.getInstance(timeZone)
        val result = mutableListOf<Date>()
        var d = startOfDay(from, timeZone)
        val end = startOfDay(to, timeZone)
        while (d < end) {
            val sb = dayStemBranch(on = d, timeZone = timeZone)
            if (sb.stem == HeavenlyStem.JIA || sb.stem == HeavenlyStem.JI) {
                result.add(d)
            }
            cal.time = d
            cal.add(Calendar.DAY_OF_MONTH, 1)
            d = cal.time
        }
        return result
    }

    fun dayStemBranch(on: Date, timeZone: TimeZone): StemBranch {
        val cal = Calendar.getInstance(timeZone)
        cal.time = on
        return GanzhiCalendar.dayStemBranch(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
            hour = 12,
            timeZone = timeZone,
        )
    }

    private fun calendarDate(year: Int, month: Int, day: Int, timeZone: TimeZone): Date {
        val cal = Calendar.getInstance(timeZone)
        cal.set(year, month - 1, day, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }
}
