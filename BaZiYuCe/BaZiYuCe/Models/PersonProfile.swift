//
//  PersonProfile.swift
//  BaZiYuCe
//
//  多人档案：每人独立出生信息与排盘结果。
//

import Foundation

struct PersonProfile: Identifiable, Codable, Hashable {
    var id: UUID
    var name: String
    var birthYear: Int
    var birthMonth: Int
    var birthDay: Int
    var birthShiChenIndex: Int
    var gender: Gender
    var createdAt: Date
    var updatedAt: Date

    init(
        id: UUID = UUID(),
        name: String,
        birthYear: Int,
        birthMonth: Int,
        birthDay: Int,
        birthShiChenIndex: Int,
        gender: Gender,
        createdAt: Date = Date(),
        updatedAt: Date = Date()
    ) {
        self.id = id
        self.name = name
        self.birthYear = birthYear
        self.birthMonth = birthMonth
        self.birthDay = birthDay
        self.birthShiChenIndex = birthShiChenIndex
        self.gender = gender
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }

    var shiChen: ShiChen {
        ShiChen.all[max(0, min(11, birthShiChenIndex))]
    }

    var summaryLine: String {
        "\(birthYear)年\(birthMonth)月\(birthDay)日 \(shiChen.branch.name)时 · \(gender.displayName)"
    }
}

struct DayStrength: Identifiable, Hashable {
    let date: Date
    let score: Int
    let flowingDay: StemBranch
    let tone: String

    var id: Date { date }
}

enum TimelineDirection: String, CaseIterable, Identifiable {
    case past
    case present
    case future

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .past: return "往昔"
        case .present: return "当下"
        case .future: return "前瞻"
        }
    }
}
