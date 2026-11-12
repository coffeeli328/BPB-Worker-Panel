package com.gglee.baziyuce

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.gglee.baziyuce.data.PeopleStore
import com.gglee.baziyuce.data.PersonProfile
import com.gglee.baziyuce.engine.BaZiChart
import com.gglee.baziyuce.engine.BaZiEngine
import com.gglee.baziyuce.engine.CalendarStrength
import com.gglee.baziyuce.engine.DayStrength
import com.gglee.baziyuce.engine.EarthlyBranch
import com.gglee.baziyuce.engine.FortuneForecast
import com.gglee.baziyuce.engine.FortunePeriod
import com.gglee.baziyuce.engine.Gender
import com.gglee.baziyuce.engine.MonthStrength
import com.gglee.baziyuce.engine.PredictionEngine
import com.gglee.baziyuce.engine.ShiChen
import com.gglee.baziyuce.engine.YearStrength
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.UUID

/** 与 iOS BaZiSession 同字段；扩展多人员档案与日历强弱。 */
class BaZiSessionViewModel : ViewModel() {

    private var store: PeopleStore? = null

    var people: List<PersonProfile> by mutableStateOf(emptyList())
        private set

    var activePersonId: String? by mutableStateOf(null)
        private set

    var personName: String by mutableStateOf("")
        private set

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

    var calendarYear: Int by mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR))
        private set

    var calendarMonth: Int by mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1)
        private set

    var dayStrengths: List<DayStrength> by mutableStateOf(emptyList())
        private set

    var monthStrengths: List<MonthStrength> by mutableStateOf(emptyList())
        private set

    var yearStrengths: List<YearStrength> by mutableStateOf(emptyList())
        private set

    var selectedDayDetail: FortuneForecast? by mutableStateOf(null)
        private set

    val timeZone: TimeZone get() = TimeZone.getDefault()

    val birthShiChen: ShiChen
        get() = ShiChen.all[birthShiChenIndex.coerceIn(0, 11)]

    val maxDayInMonth: Int
        get() = daysInMonth(birthYear, birthMonth, timeZone)

    val activePerson: PersonProfile?
        get() = people.firstOrNull { it.id == activePersonId }

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
        get() {
            val who = personName.trim().ifEmpty { null }
            val core = "${birthYear}年${birthMonth}月${birthDay}日 ${birthShiChen.branch.displayName}时 · ${gender.displayName}"
            return if (who != null) "$who · $core" else core
        }

    /** Call once from Activity/Compose with applicationContext. */
    fun attachStore(context: Context) {
        if (store != null) return
        val s = PeopleStore(context.applicationContext)
        store = s
        people = s.loadPeople()
        activePersonId = s.loadActiveId()?.takeIf { id -> people.any { it.id == id } }
            ?: people.firstOrNull()?.id
        activePerson?.let { applyProfile(it, recalculate = true) }
    }

    fun updatePersonName(value: String) {
        personName = value.take(24)
        inputError = null
    }

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
    fun calculate(saveProfile: Boolean = true): Boolean {
        if (!validateAndComposeBirthDate()) return false
        val c = BaZiEngine.chart(birthDate = birthDate, gender = gender, timeZone = timeZone)
        chart = c
        forecasts = PredictionEngine.allForecasts(chart = c, at = referenceDate, timeZone = timeZone)
        didCalculate = true
        if (saveProfile) {
            upsertCurrentAsProfile()
        }
        refreshCalendarStrengths()
        return true
    }

    fun refreshForecasts() {
        val c = chart ?: return
        forecasts = PredictionEngine.allForecasts(chart = c, at = referenceDate, timeZone = timeZone)
    }

    fun forecast(period: FortunePeriod): FortuneForecast? =
        forecasts.firstOrNull { it.period == period }

    fun selectPerson(id: String) {
        val p = people.firstOrNull { it.id == id } ?: return
        activePersonId = id
        store?.saveActiveId(id)
        applyProfile(p, recalculate = true)
    }

    fun startNewPerson() {
        activePersonId = null
        store?.saveActiveId(null)
        personName = ""
        birthYear = 1990
        birthMonth = 5
        birthDay = 15
        birthShiChenIndex = EarthlyBranch.SI.ordinal
        gender = Gender.MALE
        chart = null
        forecasts = emptyList()
        didCalculate = false
        inputError = null
        selectedDayDetail = null
        dayStrengths = emptyList()
        monthStrengths = emptyList()
        yearStrengths = emptyList()
        val now = Calendar.getInstance(timeZone)
        calendarYear = now.get(Calendar.YEAR)
        calendarMonth = now.get(Calendar.MONTH) + 1
        referenceDate = Date()
    }

    fun deletePerson(id: String) {
        val next = people.filterNot { it.id == id }
        people = next
        store?.savePeople(next)
        if (activePersonId == id) {
            val fallback = next.firstOrNull()
            if (fallback != null) {
                selectPerson(fallback.id)
            } else {
                startNewPerson()
            }
        }
    }

    fun shiftCalendarMonth(delta: Int) {
        var y = calendarYear
        var m = calendarMonth + delta
        while (m < 1) {
            m += 12
            y -= 1
        }
        while (m > 12) {
            m -= 12
            y += 1
        }
        y = y.coerceIn(MIN_YEAR, MAX_YEAR)
        calendarYear = y
        calendarMonth = m
        refreshCalendarStrengths()
    }

    fun shiftCalendarYear(delta: Int) {
        calendarYear = (calendarYear + delta).coerceIn(MIN_YEAR, MAX_YEAR)
        refreshCalendarStrengths()
    }

    fun openDayDetail(year: Int, month: Int, day: Int) {
        val c = chart ?: return
        val at = CalendarStrength.dateAtNoon(year, month, day, timeZone)
        referenceDate = at
        refreshForecasts()
        selectedDayDetail = PredictionEngine.forecast(c, FortunePeriod.DAY, at, timeZone)
    }

    fun openMonthDetail(year: Int, month: Int) {
        val c = chart ?: return
        val at = CalendarStrength.dateAtNoon(year, month, 15, timeZone)
        referenceDate = at
        refreshForecasts()
        selectedDayDetail = PredictionEngine.forecast(c, FortunePeriod.MONTH, at, timeZone)
    }

    fun openYearDetail(year: Int) {
        val c = chart ?: return
        val cal = Calendar.getInstance(timeZone)
        cal.clear()
        cal.set(year, Calendar.JUNE, 15, 12, 0, 0)
        val at = cal.time
        referenceDate = at
        refreshForecasts()
        selectedDayDetail = PredictionEngine.forecast(c, FortunePeriod.YEAR, at, timeZone)
    }

    fun clearDayDetail() {
        selectedDayDetail = null
    }

    fun refreshCalendarStrengths() {
        val c = chart ?: run {
            dayStrengths = emptyList()
            monthStrengths = emptyList()
            yearStrengths = emptyList()
            return
        }
        dayStrengths = CalendarStrength.dayStrengthsForMonth(c, calendarYear, calendarMonth, timeZone)
        monthStrengths = CalendarStrength.monthStrengthsForYear(c, calendarYear, timeZone)
        yearStrengths = CalendarStrength.yearStrengths(c, calendarYear, span = 5, timeZone = timeZone)
    }

    private fun upsertCurrentAsProfile() {
        val name = personName.trim().ifEmpty { "未命名" }
        val id = activePersonId ?: UUID.randomUUID().toString()
        val profile = PersonProfile(
            id = id,
            name = name,
            birthYear = birthYear,
            birthMonth = birthMonth,
            birthDay = birthDay.coerceIn(1, maxDayInMonth),
            birthShiChenIndex = birthShiChenIndex,
            gender = gender,
        )
        val next = people.filterNot { it.id == id } + profile
        people = next.sortedByDescending { it.updatedAtMs }
        activePersonId = id
        personName = name
        store?.savePeople(people)
        store?.saveActiveId(id)
    }

    private fun applyProfile(p: PersonProfile, recalculate: Boolean) {
        personName = p.name
        birthYear = p.birthYear
        birthMonth = p.birthMonth
        birthDay = p.birthDay
        birthShiChenIndex = p.birthShiChenIndex
        gender = p.gender
        clampBirthDay()
        inputError = null
        if (recalculate) {
            calculate(saveProfile = false)
        }
    }

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
