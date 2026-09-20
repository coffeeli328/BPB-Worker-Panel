//
//  AppTheme.swift
//  QimenDunjia
//

import SwiftUI

enum AppTheme {
    static let ink = Color(red: 0.14, green: 0.16, blue: 0.14)
    static let paper = Color(red: 0.96, green: 0.94, blue: 0.90)
    static let wash = Color(red: 0.90, green: 0.88, blue: 0.82)
    static let cinnabar = Color(red: 0.62, green: 0.22, blue: 0.18)
    static let pine = Color(red: 0.22, green: 0.38, blue: 0.32)
    static let muted = Color(red: 0.45, green: 0.42, blue: 0.38)

    static let titleFont = Font.system(.largeTitle, design: .serif).weight(.semibold)
    static let headlineFont = Font.system(.title2, design: .serif).weight(.medium)
    static let bodyFont = Font.system(.body, design: .default)
}
