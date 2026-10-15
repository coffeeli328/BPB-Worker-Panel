package com.gglee.baziyuce

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.gglee.baziyuce.engine.BaZiChart
import com.gglee.baziyuce.engine.BaZiEngine
import com.gglee.baziyuce.engine.FortuneForecast
import com.gglee.baziyuce.engine.FortunePeriod
import com.gglee.baziyuce.engine.Gender
import com.gglee.baziyuce.engine.PredictionEngine
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class BaZiSessionViewModel : ViewModel() {

    /** 公历年；引擎与节气在 1900–2100 较稳 */
    var birthYear: Int by mutableIntStateOf(1990)
        private set

    var birthMonth: Int by mutableIntStateOf(5)
        private set

    var birthDay: Int by mutableIntStateOf(15)
        private set

    /** 0–23，用于五鼠遁与子时换日 */
    var birthHour: Int by mutableIntStateOf(10)
        private set

    var gender: Gender by mutableStateOf(Gender.MALE)
        private set

    var chart: BaZiChart? by mutableStateOf(null)
        private set

    var forecasts: List<FortuneForecast> by mutableStateOf(emptyList())
        private set

    var referenceDate: Date by mutableStateOf(Date())
        private set

    var didCalculate: Boolean by mutableStateOf(false)
        private set

    var birthInputError: String? by mutableStateOf(null)
        private set

    val timeZone: TimeZone get() = TimeZone.getDefault()

    val birthDate: Date
        get() = composeBirthDate(birthYear, birthMonth, birthDay, birthHour, timeZone)
            ?: defaultBirth()

    fun updateBirthYear(value: Int) {
        birthYear = value
        clampDayIfNeeded()
        birthInputError = null
    }

    fun updateBirthMonth(value: Int) {
        birthMonth = value.coerceIn(1, 12)
        clampDayIfNeeded()
        birthInputError = null
    }

    fun updateBirthDay(value: Int) {
        birthDay = value
        birthInputError = null
    }

    fun updateBirthHour(value: Int) {
        birthHour = value.coerceIn(0, 23)
        birthInputError = null
    }

    fun updateGender(value: Gender) {
        gender = value
    }

    fun updateReferenceDate(date: Date) {
        referenceDate = date
        refreshForecasts()
    }

    /** @return true if chart was calculated */
    fun calculate(): Boolean {
        val error = validateBirthFields(birthYear, birthMonth, birthDay, birthHour)
        if (error != null) {
            birthInputError = error
            return false
        }
        val date = composeBirthDate(birthYear, birthMonth, birthDay, birthHour, timeZone)
        if (date == null) {
            birthInputError = "日期无效，请检查年、月、日"
            return false
        }
        birthInputError = null
        val c = BaZiEngine.chart(birthDate = date, gender = gender, timeZone = timeZone)
        chart = c
        forecasts = PredictionEngine.allForecasts(chart = c, at = referenceDate, timeZone = timeZone)
        didCalculate = true
        return true
    }

    fun refreshForecasts() {
        val c = chart ?: return
        forecasts = PredictionEngine.allForecasts(chart = c, at = referenceDate, timeZone = timeZone)
    }

    fun forecast(period: FortunePeriod): FortuneForecast? =
        forecasts.firstOrNull { it.period == period }

    private fun clampDayIfNeeded() {
        val max = daysInMonth(birthYear, birthMonth)
        if (birthDay > max) birthDay = max
    }

    companion object {
        const val MIN_YEAR = 1900
        const val MAX_YEAR = 2100

        fun daysInMonth(year: Int, month: Int): Int {
            if (month !in 1..12) return 31
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.set(Calendar.YEAR, year.coerceIn(MIN_YEAR, MAX_YEAR))
            cal.set(Calendar.MONTH, month - 1)
            return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        }

        fun validateBirthFields(year: Int, month: Int, day: Int, hour: Int): String? {
            if (year !in MIN_YEAR..MAX_YEAR) {
                return "年份请在 $MIN_YEAR–$MAX_YEAR 之间"
            }
            if (month !in 1..12) {
                return "月份请在 1–12 之间"
            }
            val maxDay = daysInMonth(year, month)
            if (day !in 1..maxDay) {
                return "日期请在 1–$maxDay 之间（$year 年 $month 月）"
            }
            if (hour !in 0..23) {
                return "时辰请选择 0–23 点"
            }
            return null
        }

        fun composeBirthDate(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            timeZone: TimeZone = TimeZone.getDefault(),
        ): Date? {
            if (validateBirthFields(year, month, day, hour) != null) return null
            val cal = Calendar.getInstance(timeZone)
            cal.clear()
            cal.timeZone = timeZone
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month - 1)
            cal.set(Calendar.DAY_OF_MONTH, day)
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.time
        }

        private fun defaultBirth(): Date {
            val cal = Calendar.getInstance()
            cal.set(1990, Calendar.MAY, 15, 10, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.time
        }

        /** 十二时辰：显示名 → 代表钟点（该时辰起始或中位，与 hourBranch 一致） */
        val SHICHEN_OPTIONS: List<Pair<Int, String>> = listOf(
            0 to "子时（23–01，取0点）",
            2 to "丑时（01–03）",
            4 to "寅时（03–05）",
            6 to "卯时（05–07）",
            8 to "辰时（07–09）",
            10 to "巳时（09–11）",
            12 to "午时（11–13）",
            14 to "未时（13–15）",
            16 to "申时（15–17）",
            18 to "酉时（17–19）",
            20 to "戌时（19–21）",
            22 to "亥时（21–23）",
        )

        fun shichenLabelForHour(hour: Int): String {
            val h = hour.coerceIn(0, 23)
            // Prefer exact match to option hours; else nearest even shichen start
            val exact = SHICHEN_OPTIONS.firstOrNull { it.first == h }
            if (exact != null) return exact.second
            // Map via same formula as EarthlyBranch.hourBranch
            val idx = ((h + 1) / 2) % 12
            val startHour = if (idx == 0) 0 else idx * 2
            return SHICHEN_OPTIONS.firstOrNull { it.first == startHour }?.second
                ?: "${h}点"
        }
    }
}
