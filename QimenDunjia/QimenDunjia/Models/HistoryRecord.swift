//
//  HistoryRecord.swift
//  QimenDunjia
//

import Foundation
import SwiftData

@Model
final class HistoryRecord {
    var id: UUID
    var createdAt: Date
    var queryDate: Date
    var summary: String
    var chartJSON: Data

    init(chart: QimenChart) {
        self.id = chart.id
        self.createdAt = chart.createdAt
        self.queryDate = chart.queryDate
        self.summary = "\(chart.juTitle) · \(chart.hourSB.name)时"
        let data = (try? JSONEncoder().encode(chart)) ?? Data()
        self.chartJSON = data
    }

    func decodedChart() -> QimenChart? {
        try? JSONDecoder().decode(QimenChart.self, from: chartJSON)
    }
}
