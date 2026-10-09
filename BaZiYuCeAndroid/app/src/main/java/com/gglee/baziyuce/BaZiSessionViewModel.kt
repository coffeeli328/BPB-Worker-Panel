package com.gglee.baziyuce

import androidx.compose.runtime.getValue
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

    var birthDate: Date by mutableStateOf(defaultBirth())
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

    val timeZone: TimeZone get() = TimeZone.getDefault()

    fun updateBirthDate(date: Date) {
        birthDate = date
    }

    fun updateGender(value: Gender) {
        gender = value
    }

    fun updateReferenceDate(date: Date) {
        referenceDate = date
        refreshForecasts()
    }

    fun calculate() {
        val c = BaZiEngine.chart(birthDate = birthDate, gender = gender, timeZone = timeZone)
        chart = c
        forecasts = PredictionEngine.allForecasts(chart = c, at = referenceDate, timeZone = timeZone)
        didCalculate = true
    }

    fun refreshForecasts() {
        val c = chart ?: return
        forecasts = PredictionEngine.allForecasts(chart = c, at = referenceDate, timeZone = timeZone)
    }

    fun forecast(period: FortunePeriod): FortuneForecast? =
        forecasts.firstOrNull { it.period == period }

    private fun defaultBirth(): Date {
        val cal = Calendar.getInstance()
        cal.set(1990, Calendar.MAY, 15, 10, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }
}
