//
//  AppTheme.swift
//  QimenDunjia
//
//  Calm traditional-meets-modern: ink on warm paper, pine/cinnabar accents.
//

import SwiftUI

enum AppTheme {
    static let ink = Color(red: 0.12, green: 0.14, blue: 0.12)
    static let paper = Color(red: 0.97, green: 0.95, blue: 0.91)
    static let wash = Color(red: 0.93, green: 0.90, blue: 0.84)
    static let washDeep = Color(red: 0.88, green: 0.84, blue: 0.76)
    static let line = Color(red: 0.22, green: 0.24, blue: 0.20).opacity(0.28)
    static let cinnabar = Color(red: 0.58, green: 0.20, blue: 0.16)
    static let pine = Color(red: 0.20, green: 0.36, blue: 0.30)
    static let muted = Color(red: 0.42, green: 0.40, blue: 0.36)
    static let heavenStem = Color(red: 0.10, green: 0.12, blue: 0.10)
    static let earthStem = Color(red: 0.40, green: 0.38, blue: 0.34)

    static let titleFont = Font.system(size: 34, weight: .semibold, design: .serif)
    static let headlineFont = Font.system(size: 22, weight: .medium, design: .serif)
    static let sectionFont = Font.system(size: 13, weight: .semibold, design: .serif)
    static let bodyFont = Font.system(.body, design: .default)

    /// Soft atmospheric wash behind screens (not a hero image).
    static var screenBackground: some View {
        ZStack {
            paper
            LinearGradient(
                colors: [
                    Color(red: 0.94, green: 0.91, blue: 0.84).opacity(0.9),
                    paper,
                    Color(red: 0.91, green: 0.93, blue: 0.90).opacity(0.55)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        }
        .ignoresSafeArea()
    }
}
