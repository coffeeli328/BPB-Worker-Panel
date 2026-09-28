//
//  AppTheme.swift
//  BaZiYuCe
//
//  Cool mist + celadon ink — distinct from the Qimen warm-paper look.
//

import SwiftUI

enum AppTheme {
    static let ink = Color(red: 0.10, green: 0.14, blue: 0.16)
    static let mist = Color(red: 0.90, green: 0.93, blue: 0.94)
    static let mistDeep = Color(red: 0.78, green: 0.85, blue: 0.86)
    static let celadon = Color(red: 0.22, green: 0.42, blue: 0.38)
    static let celadonSoft = Color(red: 0.35, green: 0.55, blue: 0.50)
    static let slate = Color(red: 0.36, green: 0.42, blue: 0.44)
    static let line = Color(red: 0.18, green: 0.28, blue: 0.30).opacity(0.22)
    static let accent = Color(red: 0.55, green: 0.38, blue: 0.22) // muted bronze, sparingly

    static let brandFont = Font.system(size: 36, weight: .semibold, design: .serif)
    static let titleFont = Font.system(size: 26, weight: .semibold, design: .serif)
    static let headlineFont = Font.system(size: 20, weight: .medium, design: .serif)
    static let sectionFont = Font.system(size: 13, weight: .semibold, design: .rounded)
    static let bodyFont = Font.system(.body, design: .default)
    static let pillarFont = Font.system(size: 28, weight: .medium, design: .serif)

    static var screenBackground: some View {
        ZStack {
            mist
            LinearGradient(
                colors: [
                    Color(red: 0.82, green: 0.90, blue: 0.91).opacity(0.95),
                    mist,
                    Color(red: 0.88, green: 0.90, blue: 0.86).opacity(0.7)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            // subtle radial wash
            RadialGradient(
                colors: [
                    celadon.opacity(0.08),
                    .clear
                ],
                center: .topTrailing,
                startRadius: 20,
                endRadius: 420
            )
        }
        .ignoresSafeArea()
    }
}
