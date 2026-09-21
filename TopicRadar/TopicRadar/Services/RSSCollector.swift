//
//  RSSCollector.swift
//  TopicRadar
//

import Foundation
import SwiftData

struct ParsedRSSItem {
    var title: String
    var link: String
    var summary: String
    var publishedAt: Date?
}

enum RSSCollector {
    struct Result {
        var items: [ParsedRSSItem]
        var error: String?
    }

    static func collect(topic: RadarTopic, context: ModelContext) async -> CollectSummary {
        var summary = CollectSummary()
        let enabledSources = topic.sources.filter(\.enabled)
        let now = Date()

        for source in enabledSources {
            let result = await fetchSource(source)
            source.lastFetchedAt = now
            source.lastError = result.error
            if let error = result.error {
                summary.sourceErrors.append((name: source.name, error: error))
            }

            for item in result.items {
                let matched = matchedKeywords(in: "\(item.title) \(item.summary)", keywords: topic.keywords)
                let keep = shouldKeep(
                    title: item.title,
                    summary: item.summary,
                    matched: matched,
                    source: source
                )
                guard keep else { continue }
                summary.fetched += 1

                if let existing = topic.articles.first(where: { $0.urlString == item.link }) {
                    existing.title = item.title
                    existing.summary = String(item.summary.prefix(500))
                    existing.sourceName = source.name
                    existing.sourceId = source.id
                    existing.publishedAt = item.publishedAt ?? existing.publishedAt
                    existing.matchedKeywordsRaw = matched.joined(separator: ",")
                    summary.updated += 1
                } else {
                    let article = RadarArticle(
                        title: item.title,
                        urlString: item.link,
                        summary: String(item.summary.prefix(500)),
                        sourceName: source.name,
                        sourceId: source.id,
                        publishedAt: item.publishedAt,
                        collectedAt: now,
                        matchedKeywords: matched
                    )
                    article.topic = topic
                    context.insert(article)
                    summary.added += 1
                }
            }
        }

        topic.lastCollectedAt = now
        topic.updatedAt = now
        try? context.save()
        return summary
    }

    static func shouldKeep(title: String, summary: String, matched: [String], source: RadarSource) -> Bool {
        if !matched.isEmpty { return true }
        if source.isTopicScopedFeed { return !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
        return false
    }

    static func matchedKeywords(in text: String, keywords: [String]) -> [String] {
        let haystack = normalize(text)
        return keywords.filter { haystack.contains(normalize($0)) }
    }

    private static func normalize(_ value: String) -> String {
        value.lowercased().replacingOccurrences(of: "\\s+", with: " ", options: .regularExpression)
    }

    private static func fetchSource(_ source: RadarSource) async -> Result {
        guard let url = source.url else {
            return Result(items: [], error: "无效的 URL")
        }

        var request = URLRequest(url: url, timeoutInterval: 15)
        request.setValue(
            "Mozilla/5.0 (compatible; TopicRadar/1.0; +https://localhost; RSS reader)",
            forHTTPHeaderField: "User-Agent"
        )
        request.setValue(
            "application/rss+xml, application/atom+xml, application/xml, text/xml, */*",
            forHTTPHeaderField: "Accept"
        )

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            if let http = response as? HTTPURLResponse, !(200...299).contains(http.statusCode) {
                return Result(items: [], error: "HTTP \(http.statusCode)")
            }
            let items = try RSSFeedParser.parse(data: data)
            return Result(items: items, error: nil)
        } catch {
            return Result(items: [], error: error.localizedDescription)
        }
    }
}

final class RSSFeedParser: NSObject, XMLParserDelegate {
    private var items: [ParsedRSSItem] = []
    private var currentElement = ""
    private var currentTitle = ""
    private var currentLink = ""
    private var currentSummary = ""
    private var currentDateRaw = ""
    private var insideItem = false
    private var parseError: Error?

    static func parse(data: Data) throws -> [ParsedRSSItem] {
        let parser = RSSFeedParser()
        let xml = XMLParser(data: data)
        xml.delegate = parser
        guard xml.parse() else {
            throw parser.parseError ?? NSError(domain: "RSSFeedParser", code: 1, userInfo: [NSLocalizedDescriptionKey: "RSS 解析失败"])
        }
        return parser.items
    }

    func parser(_ parser: XMLParser, didStartElement elementName: String, namespaceURI: String?, qualifiedName qName: String?, attributes attributeDict: [String: String] = [:]) {
        currentElement = elementName.lowercased()
        if currentElement == "item" || currentElement == "entry" {
            insideItem = true
            currentTitle = ""
            currentLink = ""
            currentSummary = ""
            currentDateRaw = ""
        }
        if insideItem, currentElement == "link", let href = attributeDict["href"], currentLink.isEmpty {
            currentLink = href
        }
    }

    func parser(_ parser: XMLParser, foundCharacters string: String) {
        guard insideItem else { return }
        switch currentElement {
        case "title":
            currentTitle += string
        case "link":
            currentLink += string
        case "description", "summary", "content", "content:encoded":
            currentSummary += string
        case "pubdate", "published", "updated", "dc:date":
            currentDateRaw += string
        default:
            break
        }
    }

    func parser(_ parser: XMLParser, didEndElement elementName: String, namespaceURI: String?, qualifiedName qName: String?) {
        let name = elementName.lowercased()
        if name == "item" || name == "entry" {
            let title = currentTitle.trimmingCharacters(in: .whitespacesAndNewlines)
            let link = currentLink.trimmingCharacters(in: .whitespacesAndNewlines)
            if !title.isEmpty, !link.isEmpty {
                items.append(
                    ParsedRSSItem(
                        title: title,
                        link: link,
                        summary: stripHTML(currentSummary),
                        publishedAt: parseDate(currentDateRaw)
                    )
                )
            }
            insideItem = false
        }
        currentElement = ""
    }

    func parser(_ parser: XMLParser, parseErrorOccurred parseError: Error) {
        self.parseError = parseError
    }

    private func stripHTML(_ value: String) -> String {
        value
            .replacingOccurrences(of: "<[^>]+>", with: " ", options: .regularExpression)
            .replacingOccurrences(of: "\\s+", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func parseDate(_ raw: String) -> Date? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }

        let iso = ISO8601DateFormatter()
        iso.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = iso.date(from: trimmed) { return date }
        iso.formatOptions = [.withInternetDateTime]
        if let date = iso.date(from: trimmed) { return date }

        let rfc = DateFormatter()
        rfc.locale = Locale(identifier: "en_US_POSIX")
        rfc.dateFormat = "EEE, dd MMM yyyy HH:mm:ss Z"
        return rfc.date(from: trimmed)
    }
}
