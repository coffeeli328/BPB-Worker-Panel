//
//  TopicModels.swift
//  TopicRadar
//

import Foundation
import SwiftData

@Model
final class RadarTopic {
    @Attribute(.unique) var id: UUID
    var name: String
    var topicDescription: String
    /// Comma-separated keywords for filtering.
    var keywordsRaw: String
    var createdAt: Date
    var updatedAt: Date
    var lastCollectedAt: Date?

    @Relationship(deleteRule: .cascade, inverse: \RadarSource.topic)
    var sources: [RadarSource]

    @Relationship(deleteRule: .cascade, inverse: \RadarArticle.topic)
    var articles: [RadarArticle]

    init(
        id: UUID = UUID(),
        name: String,
        topicDescription: String = "",
        keywords: [String],
        createdAt: Date = .now,
        updatedAt: Date = .now,
        lastCollectedAt: Date? = nil,
        sources: [RadarSource] = [],
        articles: [RadarArticle] = []
    ) {
        self.id = id
        self.name = name
        self.topicDescription = topicDescription
        self.keywordsRaw = keywords.joined(separator: ",")
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.lastCollectedAt = lastCollectedAt
        self.sources = sources
        self.articles = articles
    }

    var keywords: [String] {
        keywordsRaw
            .split(separator: ",")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
    }

    var sortedArticles: [RadarArticle] {
        articles.sorted { lhs, rhs in
            let left = lhs.publishedAt ?? lhs.collectedAt
            let right = rhs.publishedAt ?? rhs.collectedAt
            return left > right
        }
    }
}

@Model
final class RadarSource {
    @Attribute(.unique) var id: UUID
    var name: String
    var urlString: String
    var enabled: Bool
    var lastFetchedAt: Date?
    var lastError: String?
    var topic: RadarTopic?

    init(
        id: UUID = UUID(),
        name: String,
        urlString: String,
        enabled: Bool = true,
        lastFetchedAt: Date? = nil,
        lastError: String? = nil
    ) {
        self.id = id
        self.name = name
        self.urlString = urlString
        self.enabled = enabled
        self.lastFetchedAt = lastFetchedAt
        self.lastError = lastError
    }

    var url: URL? {
        URL(string: urlString)
    }

    var isTopicScopedFeed: Bool {
        let lower = urlString.lowercased()
        return lower.contains("google.com/rss/search") || lower.contains("reddit.com/r/")
    }
}

@Model
final class RadarArticle {
    @Attribute(.unique) var id: UUID
    var title: String
    var urlString: String
    var summary: String
    var sourceName: String
    var sourceId: UUID?
    var publishedAt: Date?
    var collectedAt: Date
    var matchedKeywordsRaw: String
    var topic: RadarTopic?

    init(
        id: UUID = UUID(),
        title: String,
        urlString: String,
        summary: String,
        sourceName: String,
        sourceId: UUID? = nil,
        publishedAt: Date? = nil,
        collectedAt: Date = .now,
        matchedKeywords: [String] = []
    ) {
        self.id = id
        self.title = title
        self.urlString = urlString
        self.summary = summary
        self.sourceName = sourceName
        self.sourceId = sourceId
        self.publishedAt = publishedAt
        self.collectedAt = collectedAt
        self.matchedKeywordsRaw = matchedKeywords.joined(separator: ",")
    }

    var url: URL? {
        URL(string: urlString)
    }

    var matchedKeywords: [String] {
        matchedKeywordsRaw
            .split(separator: ",")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
    }
}

struct CollectSummary {
    var fetched: Int = 0
    var added: Int = 0
    var updated: Int = 0
    var sourceErrors: [(name: String, error: String)] = []

    var message: String {
        var text = "采集完成：拉取 \(fetched) 条，新增 \(added)，更新 \(updated)"
        if !sourceErrors.isEmpty {
            text += "；\(sourceErrors.count) 个源失败"
        }
        return text
    }
}
