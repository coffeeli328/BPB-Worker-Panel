//
//  QimenDunjiaApp.swift
//  QimenDunjia
//

import SwiftUI
import SwiftData

@main
struct QimenDunjiaApp: App {
    var body: some Scene {
        WindowGroup {
            RootTabView()
                .preferredColorScheme(.light)
        }
        .modelContainer(for: HistoryRecord.self)
    }
}
