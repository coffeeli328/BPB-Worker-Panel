//
//  ChartModels.swift
//  QimenDunjia
//

import Foundation

enum CalendarInputMode: String, Codable, CaseIterable, Identifiable {
    case solar = "阳历"
    case lunar = "农历"
    var id: String { rawValue }
}

enum QimenMethod: String, Codable, CaseIterable, Identifiable {
    case shiJia = "时家奇门"
    var id: String { rawValue }
}

struct PalaceCell: Codable, Hashable, Identifiable {
    var id: Int { palace.rawValue }
    let palace: Palace
    let earthStem: HeavenlyStem?
    let heavenStem: HeavenlyStem?
    let star: NineStar?
    let gate: EightGate?
    let deity: EightDeity?
    let isEmpty: Bool
    let isZhiFu: Bool
    let isZhiShi: Bool
}

struct QimenChart: Codable, Hashable, Identifiable {
    let id: UUID
    let createdAt: Date
    let queryDate: Date
    let calendarMode: CalendarInputMode
    let timeZoneIdentifier: String
    let method: QimenMethod
    let locationNote: String
    let yearSB: StemBranch
    let monthSB: StemBranch
    let daySB: StemBranch
    let hourSB: StemBranch
    let isYangDun: Bool
    let juNumber: Int
    let solarTermName: String
    let yuanName: String
    let zhiFuStar: NineStar
    let zhiShiGate: EightGate
    let zhiFuPalace: Palace
    let zhiShiPalace: Palace
    let xunKong: [EarthlyBranch]
    let cells: [PalaceCell]
    let interpretations: [InterpretationItem]

    var juTitle: String { "\(isYangDun ? "阳遁" : "阴遁")\(juNumber)局 · \(yuanName)" }
    var ganzhiLine: String {
        "\(yearSB.name)年 \(monthSB.name)月 \(daySB.name)日 \(hourSB.name)时"
    }

    func cell(for palace: Palace) -> PalaceCell? {
        cells.first { $0.palace == palace }
    }
}

struct InterpretationItem: Codable, Hashable, Identifiable {
    let id: UUID
    let title: String
    let detail: String
    let tone: Tone

    enum Tone: String, Codable { case neutral, auspicious, caution }

    init(title: String, detail: String, tone: Tone = .neutral) {
        self.id = UUID()
        self.title = title
        self.detail = detail
        self.tone = tone
    }
}

struct ChartRequest: Hashable {
    var date: Date = Date()
    var calendarMode: CalendarInputMode = .solar
    var method: QimenMethod = .shiJia
    var timeZoneSecondsFromGMT: Int? = nil
    var locationNote: String = "系统时区"
}
