//
//  KeywordFilterTests.swift
//  TopicRadarTests
//

import XCTest
@testable import TopicRadar

final class KeywordFilterTests: XCTestCase {
    func testMatchesChineseAndEnglishKeywords() {
        let text = "Vancouver home sales fell as 温哥华 房价 cooled"
        let matched = RSSCollector.matchedKeywords(
            in: text,
            keywords: ["vancouver", "房价", "toronto", "rent"]
        )
        XCTAssertEqual(Set(matched), Set(["vancouver", "房价"]))
    }

    func testTopicScopedFeedKeepsWithoutKeywordHit() {
        let source = RadarSource(
            name: "Google News",
            urlString: "https://news.google.com/rss/search?q=test"
        )
        let keep = RSSCollector.shouldKeep(
            title: "Random housing headline",
            summary: "",
            matched: [],
            source: source
        )
        XCTAssertTrue(keep)
    }

    func testBroadFeedRequiresKeyword() {
        let source = RadarSource(
            name: "CBC",
            urlString: "https://www.cbc.ca/webfeed/rss/rss-canada-britishcolumbia"
        )
        XCTAssertFalse(
            RSSCollector.shouldKeep(title: "Weather update", summary: "Sunny", matched: [], source: source)
        )
        XCTAssertTrue(
            RSSCollector.shouldKeep(
                title: "Housing market softens",
                summary: "",
                matched: ["housing"],
                source: source
            )
        )
    }
}
