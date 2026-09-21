//
//  AppTheme.swift
//  TopicRadar
//
//  Coastal Pacific Northwest: deep ink, mist, seafoam.
//

import SwiftUI

enum AppTheme {
    static let ink = Color(red: 0.043, green: 0.122, blue: 0.165)
    static let mist = Color(red: 0.906, green: 0.949, blue: 0.945)
    static let fog = Color(red: 0.773, green: 0.867, blue: 0.851)
    static let sea = Color(red: 0.122, green: 0.435, blue: 0.416)
    static let seaBright = Color(red: 0.494, green: 0.816, blue: 0.769)
    static let sand = Color(red: 0.969, green: 0.953, blue: 0.918)
    static let signal = Color(red: 0.949, green: 0.757, blue: 0.306)
    static let muted = Color(red: 0.353, green: 0.451, blue: 0.502)
    static let line = Color(red: 0.043, green: 0.122, blue: 0.165).opacity(0.12)
    static let danger = Color(red: 0.769, green: 0.361, blue: 0.290)

    static let brandFont = Font.system(size: 40, weight: .bold, design: .rounded)
    static let titleFont = Font.system(size: 28, weight: .bold, design: .rounded)
    static let headlineFont = Font.system(size: 20, weight: .semibold, design: .rounded)
    static let bodyFont = Font.system(.body, design: .default)

    static var screenBackground: some View {
        ZStack {
            mist
            LinearGradient(
                colors: [
                    seaBright.opacity(0.28),
                    mist,
                    Color(red: 0.953, green: 0.969, blue: 0.965)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            RadialGradient(
                colors: [signal.opacity(0.14), .clear],
                center: .topTrailing,
                startRadius: 20,
                endRadius: 280
            )
        }
        .ignoresSafeArea()
    }
}
