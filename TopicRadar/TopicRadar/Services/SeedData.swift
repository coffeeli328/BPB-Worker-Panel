//
//  SeedData.swift
//  TopicRadar
//

import Foundation
import SwiftData

enum SeedData {
    static let vancouverKeywords = [
        "温哥华", "大温", "房价", "楼市", "房产", "按揭",
        "condo", "housing", "real estate", "home sales", "home prices",
        "mortgage", "vancouver", "metro vancouver", "greater vancouver",
        "benchmark", "listing"
    ]

    static func ensureSeedIfNeeded(context: ModelContext) throws {
        let descriptor = FetchDescriptor<RadarTopic>()
        let existing = try context.fetch(descriptor)
        guard existing.isEmpty else { return }

        let topic = RadarTopic(
            name: "温哥华房价",
            topicDescription: "聚合温哥华及大温地区房价、成交量、新盘与政策相关报道。",
            keywords: vancouverKeywords,
            sources: [
                RadarSource(
                    name: "Google News · Vancouver housing",
                    urlString: "https://news.google.com/rss/search?q=Vancouver+housing+prices&hl=en-CA&gl=CA&ceid=CA:en"
                ),
                RadarSource(
                    name: "Google News · 温哥华房价",
                    urlString: "https://news.google.com/rss/search?q=%E6%B8%A9%E5%93%A5%E5%8D%8E+%E6%88%BF%E4%BB%B7&hl=zh-CN&gl=CA&ceid=CA:zh-Hans"
                ),
                RadarSource(
                    name: "CBC British Columbia",
                    urlString: "https://www.cbc.ca/webfeed/rss/rss-canada-britishcolumbia"
                ),
                RadarSource(
                    name: "Reddit · r/vancouverhousing",
                    urlString: "https://www.reddit.com/r/vancouverhousing/.rss"
                )
            ]
        )
        context.insert(topic)
        try context.save()
    }
}
