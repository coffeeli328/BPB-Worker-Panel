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
    /// 所问之事（冗余便于列表；完整盘在 chartJSON）
    var question: String = ""
    var chartJSON: Data
    /// 最近一次 AI 解读正文（可选；无 Key / 失败时为空）
    var aiReadingText: String = ""
    /// 与 question+盘面+model 对应的缓存键
    var aiReadingCacheKey: String = ""
    var aiReadingUpdatedAt: Date?

    init(chart: QimenChart) {
        self.id = chart.id
        self.createdAt = chart.createdAt
        self.queryDate = chart.queryDate
        self.question = chart.question
        if chart.hasQuestion {
            let shortQ = chart.question.count > 16
                ? String(chart.question.prefix(16)) + "…"
                : chart.question
            self.summary = "\(shortQ) · \(chart.juTitle)"
        } else {
            self.summary = "\(chart.juTitle) · \(chart.hourSB.name)时"
        }
        let data = (try? JSONEncoder().encode(chart)) ?? Data()
        self.chartJSON = data
        self.aiReadingText = ""
        self.aiReadingCacheKey = ""
        self.aiReadingUpdatedAt = nil
    }

    func decodedChart() -> QimenChart? {
        try? JSONDecoder().decode(QimenChart.self, from: chartJSON)
    }

    func saveAIReading(text: String, cacheKey: String) {
        aiReadingText = text
        aiReadingCacheKey = cacheKey
        aiReadingUpdatedAt = Date()
    }
}
