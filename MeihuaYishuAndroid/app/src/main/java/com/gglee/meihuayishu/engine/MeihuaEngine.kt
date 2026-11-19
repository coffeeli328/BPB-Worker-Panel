package com.gglee.meihuayishu.engine

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.floor
import kotlin.random.Random

data class CastRequest(
    var question: String = "",
    var method: CastMethod = CastMethod.TIME,
    var dateMillis: Long = System.currentTimeMillis(),
    var useLunarApprox: Boolean = false,
    var numberUpper: String = "",
    var numberLower: String = "",
    var numberMoving: String = "",
    var manualUpper: Int = 1,
    var manualLower: Int = 1,
    var manualMoving: Int = 1,
)

object MeihuaEngine {

    fun cast(request: CastRequest, timeZone: TimeZone = TimeZone.getDefault()): MeihuaChart {
        val question = request.question.trim()
        return when (request.method) {
            CastMethod.TIME -> castByTime(question, Date(request.dateMillis), timeZone)
            CastMethod.NUMBER -> castByNumbers(
                question = question,
                upperRaw = request.numberUpper,
                lowerRaw = request.numberLower,
                movingRaw = request.numberMoving,
            )
            CastMethod.MANUAL -> castManual(
                question = question,
                upperNum = request.manualUpper,
                lowerNum = request.manualLower,
                moving = request.manualMoving,
            )
            CastMethod.RANDOM -> castRandom(question)
        }
    }

    fun castByTime(question: String, date: Date, timeZone: TimeZone): MeihuaChart {
        val cal = Calendar.getInstance(timeZone).apply { time = date }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        val yearBranch = yearBranchNumber(year, month, day)
        val hourBranch = hourBranchNumber(hour)
        val upperSum = yearBranch + month + day
        val lowerSum = yearBranch + month + day + hourBranch
        val upperNum = mod8(upperSum)
        val lowerNum = mod8(lowerSum)
        val moving = mod6(lowerSum)

        val pillars = ganzhiLine(date, timeZone)
        val detail = "年支$yearBranch + 月$month + 日$day = $upperSum → 上$upperNum；" +
            "+ 时支$hourBranch = $lowerSum → 下$lowerNum；动爻$moving"

        return buildChart(
            question = question,
            method = CastMethod.TIME,
            methodDetail = detail,
            queryDate = date,
            upperNumber = upperNum,
            lowerNumber = lowerNum,
            movingLine = moving,
            yearBranchNumber = yearBranch,
            monthNumber = month,
            dayNumber = day,
            hourBranchNumber = hourBranch,
            ganzhiLine = pillars,
        )
    }

    fun castByNumbers(
        question: String,
        upperRaw: String,
        lowerRaw: String,
        movingRaw: String,
        queryDate: Date = Date(),
    ): MeihuaChart {
        val u = upperRaw.trim().toIntOrNull()
            ?: throw IllegalArgumentException("请输入有效的上卦数字")
        val l = lowerRaw.trim().toIntOrNull()
            ?: throw IllegalArgumentException("请输入有效的下卦数字")
        require(u > 0 && l > 0) { "数字须为正整数" }
        val upperNum = mod8(u)
        val lowerNum = mod8(l)
        val moving = if (movingRaw.isNotBlank()) {
            val m = movingRaw.trim().toIntOrNull()
                ?: throw IllegalArgumentException("动爻数字无效")
            require(m > 0) { "动爻须为正整数" }
            mod6(m)
        } else {
            mod6(u + l)
        }
        val detail = "上数$u → $upperNum；下数$l → $lowerNum；动爻${if (movingRaw.isBlank()) "$u+$l" else movingRaw} → $moving"
        return buildChart(
            question = question,
            method = CastMethod.NUMBER,
            methodDetail = detail,
            queryDate = queryDate,
            upperNumber = upperNum,
            lowerNumber = lowerNum,
            movingLine = moving,
            ganzhiLine = "",
        )
    }

    fun castManual(
        question: String,
        upperNum: Int,
        lowerNum: Int,
        moving: Int,
        queryDate: Date = Date(),
    ): MeihuaChart {
        require(upperNum in 1..8 && lowerNum in 1..8) { "卦数须在 1–8" }
        require(moving in 1..6) { "动爻须在 1–6" }
        return buildChart(
            question = question,
            method = CastMethod.MANUAL,
            methodDetail = "上${Trigram.fromNumber(upperNum).hanzi} 下${Trigram.fromNumber(lowerNum).hanzi} 动$moving",
            queryDate = queryDate,
            upperNumber = upperNum,
            lowerNumber = lowerNum,
            movingLine = moving,
            ganzhiLine = "",
        )
    }

    fun castRandom(question: String, random: Random = Random.Default): MeihuaChart {
        val u = random.nextInt(1, 1000)
        val l = random.nextInt(1, 1000)
        return castByNumbers(question, u.toString(), l.toString(), "", Date()).copy(
            method = CastMethod.RANDOM,
            methodDetail = "随机上$u 下$l → 上${mod8(u)} 下${mod8(l)} 动${mod6(u + l)}",
        )
    }

    fun buildChart(
        question: String,
        method: CastMethod,
        methodDetail: String,
        queryDate: Date,
        upperNumber: Int,
        lowerNumber: Int,
        movingLine: Int,
        yearBranchNumber: Int? = null,
        monthNumber: Int? = null,
        dayNumber: Int? = null,
        hourBranchNumber: Int? = null,
        ganzhiLine: String = "",
    ): MeihuaChart {
        val original = Hexagram.fromNumbers(upperNumber, lowerNumber)
        val mutual = original.mutual()
        val changed = original.withMovingLine(movingLine)
        val (ti, yong) = resolveTiYong(original, movingLine)
        val relation = ti.element.relationTo(yong.element)
        val chart = MeihuaChart(
            queryDate = queryDate,
            question = question,
            method = method,
            methodDetail = methodDetail,
            yearBranchNumber = yearBranchNumber,
            monthNumber = monthNumber,
            dayNumber = dayNumber,
            hourBranchNumber = hourBranchNumber,
            upperNumber = upperNumber,
            lowerNumber = lowerNumber,
            movingLine = movingLine,
            original = original,
            mutual = mutual,
            changed = changed,
            ti = ti,
            yong = yong,
            tiYongRelation = relation,
            ganzhiLine = ganzhiLine,
            interpretations = emptyList(),
        )
        return chart.copy(interpretations = InterpretationEngine.interpret(chart))
    }

    /** 动爻在上卦（4–6）则体为下、用为上；在下卦（1–3）则体为上、用为下 */
    fun resolveTiYong(original: Hexagram, movingLine: Int): Pair<Trigram, Trigram> {
        return if (movingLine in 4..6) {
            original.lower to original.upper
        } else {
            original.upper to original.lower
        }
    }

    fun mod8(n: Int): Int {
        val r = ((n % 8) + 8) % 8
        return if (r == 0) 8 else r
    }

    fun mod6(n: Int): Int {
        val r = ((n % 6) + 6) % 6
        return if (r == 0) 6 else r
    }

    /** 地支序数：子1…亥12；立春前归上年 */
    fun yearBranchNumber(solarYear: Int, month: Int, day: Int): Int {
        var y = solarYear
        if (month < 2 || (month == 2 && day < 4)) y -= 1
        // 1984 甲子年，子=1
        val idx = ((y - 1984) % 12 + 12) % 12
        return idx + 1
    }

    fun hourBranchNumber(hour: Int): Int {
        // 23–1 子1, 1–3 丑2 … 
        val h = if (hour == 23) 0 else hour
        return (h + 1) / 2 + 1
    }

    fun ganzhiLine(date: Date, timeZone: TimeZone): String {
        val cal = Calendar.getInstance(timeZone).apply { time = date }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val yearSB = yearStemBranch(y, m, d)
        val daySB = dayStemBranch(y, m, d, hour)
        val hourSB = hourStemBranch(daySB.first, hour)
        val monthSB = monthStemBranch(yearSB.first, m, d)
        return "${yearSB.second}${monthSB.second}${daySB.second}${hourSB.second}"
    }

    private val stems = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    private val branches = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")

    private fun yearStemBranch(solarYear: Int, month: Int, day: Int): Pair<Int, String> {
        var y = solarYear
        if (month < 2 || (month == 2 && day < 4)) y -= 1
        val idx = ((y - 1984) % 60 + 60) % 60
        return idx % 10 to stems[idx % 10] + branches[idx % 12]
    }

    private fun monthStemBranch(yearStem: Int, month: Int, day: Int): Pair<Int, String> {
        val jieMonth = approximateJieMonth(month, day)
        val startStem = when (yearStem) {
            0, 5 -> 2 // 甲己 → 丙
            1, 6 -> 4
            2, 7 -> 6
            3, 8 -> 8
            else -> 0
        }
        val stem = (startStem + jieMonth - 1) % 10
        val branch = (2 + jieMonth - 1) % 12 // 寅起
        return stem to stems[stem] + branches[branch]
    }

    private fun approximateJieMonth(month: Int, day: Int): Int {
        val boundaries = listOf(
            2 to 4, 3 to 6, 4 to 5, 5 to 6, 6 to 6, 7 to 7,
            8 to 8, 9 to 8, 10 to 8, 11 to 7, 12 to 7, 1 to 6,
        )
        val md = month * 100 + day
        if (md < 106) return 12
        for (i in 0 until 11) {
            val a = boundaries[i].first * 100 + boundaries[i].second
            val b = boundaries[i + 1].first * 100 + boundaries[i + 1].second
            if (md >= a && md < b) return i + 1
        }
        if (md >= 1207) return 11
        return 12
    }

    private fun dayStemBranch(year: Int, month: Int, day: Int, hour: Int): Pair<Int, String> {
        var y = year
        var m = month
        var d = day
        if (hour >= 23) {
            val cal = Calendar.getInstance()
            cal.set(y, m - 1, d)
            cal.add(Calendar.DAY_OF_MONTH, 1)
            y = cal.get(Calendar.YEAR)
            m = cal.get(Calendar.MONTH) + 1
            d = cal.get(Calendar.DAY_OF_MONTH)
        }
        val jd = julianDay(y, m, d)
        val baseJD = 2415021.0
        val baseIndex = 10
        val delta = floor(jd - baseJD).toInt()
        val idx = ((baseIndex + delta) % 60 + 60) % 60
        return idx % 10 to stems[idx % 10] + branches[idx % 12]
    }

    private fun hourStemBranch(dayStem: Int, hour: Int): Pair<Int, String> {
        val branchIdx = hourBranchNumber(hour) - 1
        val startStem = when (dayStem) {
            0, 5 -> 0
            1, 6 -> 2
            2, 7 -> 4
            3, 8 -> 6
            else -> 8
        }
        val stem = (startStem + branchIdx) % 10
        return stem to stems[stem] + branches[branchIdx]
    }

    private fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }
}
