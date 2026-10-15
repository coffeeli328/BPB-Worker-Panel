package com.gglee.baziyuce

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.gglee.baziyuce.engine.BaZiChart
import com.gglee.baziyuce.engine.BaZiEngine
import com.gglee.baziyuce.engine.EarthlyBranch
import com.gglee.baziyuce.engine.FortuneForecast
import com.gglee.baziyuce.engine.FortunePeriod
import com.gglee.baziyuce.engine.Gender
import com.gglee.baziyuce.engine.PredictionEngine
import com.gglee.baziyuce.engine.ShiChen
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/** 与 iOS BaZiSession 同字段与校验文案。 */
class BaZiSessionViewModel : ViewModel() {

    var birthYear: Int by mutableIntStateOf(1990)
        private set

    var birthMonth: Int by mutableIntStateOf(5)
        private set

    var birthDay: Int by mutableIntStateOf(15)
        private set

    /** EarthlyBranch.ordinal；默认巳时 */
    var birthShiChenIndex: Int by mutableIntStateOf(EarthlyBranch.SI.ordinal)
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

    var inputError: String? by mutableStateOf(null)
        private set

    val timeZone: TimeZone get() = TimeZone.getDefault()

    val birthShiChen: ShiChen
        get() = ShiChen.all[birthShiChenIndex.coerceIn(0, 11)]

    val maxDayInMonth: Int
        get() = daysInMonth(birthYear, birthMonth, timeZone)

    val birthDate: Date
        get() {
            val cal = Calendar.getInstance(timeZone)
            cal.clear()
            cal.timeZone = timeZone
            cal.set(Calendar.YEAR, birthYear)
            cal.set(Calendar.MONTH, birthMonth - 1)
            cal.set(Calendar.DAY_OF_MONTH, birthDay.coerceIn(1, maxDayInMonth))
            cal.set(Calendar.HOUR_OF_DAY, birthShiChen.representativeHour)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.time
        }

    val birthSummary: String
        get() = "${birthYear}年${birthMonth}月${birthDay}日 ${birthShiChen.branch.displayName}时 · ${gender.displayName}"

    fun updateBirthYear(value: Int) {
        birthYear = value
        clampBirthDay()
        inputError = null
    }

    fun updateBirthMonth(value: Int) {
        birthMonth = value.coerceIn(1, 12)
        clampBirthDay()
        inputError = null
    }

    fun updateBirthDay(value: Int) {
        birthDay = value.coerceIn(1, maxDayInMonth)
        inputError = null
    }

    fun updateBirthShiChenIndex(value: Int) {
        birthShiChenIndex = value.coerceIn(0, 11)
        inputError = null
    }

    fun updateGender(value: Gender) {
        gender = value
    }

    fun updateReferenceDate(date: Date) {
        referenceDate = date
        refreshForecasts()
    }

    fun clampBirthDay() {
        birthDay = birthDay.coerceIn(1, maxDayInMonth)
    }

    fun validateAndComposeBirthDate(): Boolean {
        inputError = null
        if (birthYear < MIN_YEAR || birthYear > MAX_YEAR) {
            inputError = "年份请在 $MIN_YEAR–$MAX_YEAR 之间"
            return false
        }
        if (birthMonth !in 1..12) {
            inputError = "月份请在 1–12 之间"
            return false
        }
        val maxDay = daysInMonth(birthYear, birthMonth, timeZone)
        if (birthDay !in 1..maxDay) {
            inputError = "${birthYear}年${birthMonth}月只有 $maxDay 天"
            return false
        }
        if (birthShiChenIndex !in 0..11) {
            inputError = "请选择有效时辰"
            return false
        }
        return true
    }

    /** @return true if chart was calculated */
    fun calculate(): Boolean {
        if (!validateAndComposeBirthDate()) return false
        val c = BaZiEngine.chart(birthDate = birthDate, gender = gender, timeZone = timeZone)
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

    companion object {
        const val MIN_YEAR = 1900
        const val MAX_YEAR = 2100

        fun daysInMonth(year: Int, month: Int, timeZone: TimeZone = TimeZone.getDefault()): Int {
            if (month !in 1..12) return 28
            val cal = Calendar.getInstance(timeZone)
            cal.clear()
            cal.timeZone = timeZone
            cal.set(Calendar.YEAR, year.coerceIn(MIN_YEAR, MAX_YEAR))
            cal.set(Calendar.MONTH, month - 1)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
    }
}
