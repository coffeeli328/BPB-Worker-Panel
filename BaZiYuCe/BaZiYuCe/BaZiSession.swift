//
//  BaZiSession.swift
//  BaZiYuCe
//

import Foundation
import SwiftUI

@Observable
final class BaZiSession {
    var birthDate: Date = Calendar.current.date(from: DateComponents(year: 1990, month: 5, day: 15, hour: 10, minute: 0)) ?? Date()
    var gender: Gender = .male
    var chart: BaZiChart?
    var forecasts: [FortuneForecast] = []
    var referenceDate: Date = Date()

    var timeZone: TimeZone { .current }

    func calculate() {
        let c = BaZiEngine.chart(birthDate: birthDate, gender: gender, timeZone: timeZone)
        chart = c
        forecasts = PredictionEngine.allForecasts(chart: c, at: referenceDate, timeZone: timeZone)
    }

    func refreshForecasts() {
        guard let chart else { return }
        forecasts = PredictionEngine.allForecasts(chart: chart, at: referenceDate, timeZone: timeZone)
    }

    func forecast(for period: FortunePeriod) -> FortuneForecast? {
        forecasts.first { $0.period == period }
    }
}
