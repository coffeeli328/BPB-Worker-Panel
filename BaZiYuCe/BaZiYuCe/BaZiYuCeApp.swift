//
//  BaZiYuCeApp.swift
//  BaZiYuCe
//

import SwiftUI

@main
struct BaZiYuCeApp: App {
    @State private var session = BaZiSession()

    var body: some Scene {
        WindowGroup {
            RootTabView()
                .environment(session)
                .preferredColorScheme(.light)
        }
    }
}
