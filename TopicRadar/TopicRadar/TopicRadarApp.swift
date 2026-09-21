//
//  TopicRadarApp.swift
//  TopicRadar
//

import SwiftUI
import SwiftData

@main
struct TopicRadarApp: App {
    var body: some Scene {
        WindowGroup {
            RootTabView()
                .preferredColorScheme(.light)
        }
        .modelContainer(for: [RadarTopic.self, RadarSource.self, RadarArticle.self])
    }
}
