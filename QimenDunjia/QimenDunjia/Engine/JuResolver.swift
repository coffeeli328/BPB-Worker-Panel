//
//  JuResolver.swift
//  QimenDunjia
//
//  定局入口：默认拆补；可选置闰（见 ZhiYunResolver）。
//  拆补口径：符头定三元 + 「当前已交节气」定局数歌。
//

import Foundation

enum JuMethod: String, Codable, CaseIterable, Identifiable, Hashable {
    case chaiBu = "拆补"
    case zhiYun = "置闰"

    var id: String { rawValue }

    var detail: String {
        switch self {
        case .chaiBu: return "按交节时刻取本节气三元（默认）"
        case .zhiYun: return "超神接气；芒种/大雪可闰奇"
        }
    }
}

struct JuResolution: Hashable {
    let isYangDun: Bool
    let juNumber: Int
    let yuanName: String
    let yuanIndex: Int // 0上 1中 2下
    let solarTermName: String
    let fuTou: StemBranch
    var juMethod: JuMethod = .chaiBu
    var phase: ZhiYunPhase = .zhengShou
    var isRunQi: Bool = false
    var solarTermInstant: Date? = nil
}

enum JuResolver {

    /// 节气 → (阳?, 上中下元局数)
    static let termJu: [String: (isYang: Bool, jus: [Int])] = [
        "冬至": (true, [1, 7, 4]), "惊蛰": (true, [1, 7, 4]),
        "小寒": (true, [2, 8, 5]),
        "大寒": (true, [3, 9, 6]), "春分": (true, [3, 9, 6]),
        "立春": (true, [8, 5, 2]),
        "雨水": (true, [9, 6, 3]),
        "清明": (true, [4, 1, 7]), "立夏": (true, [4, 1, 7]),
        "谷雨": (true, [5, 2, 8]), "小满": (true, [5, 2, 8]),
        "芒种": (true, [6, 3, 9]),
        "夏至": (false, [9, 3, 6]), "白露": (false, [9, 3, 6]),
        "小暑": (false, [8, 2, 5]),
        "大暑": (false, [7, 1, 4]), "秋分": (false, [7, 1, 4]),
        "立秋": (false, [2, 5, 8]),
        "处暑": (false, [1, 4, 7]),
        "寒露": (false, [6, 9, 3]), "立冬": (false, [6, 9, 3]),
        "霜降": (false, [5, 8, 2]), "小雪": (false, [5, 8, 2]),
        "大雪": (false, [4, 7, 1])
    ]

    static func termJuEntry(_ name: String) -> (isYang: Bool, jus: [Int]) {
        let key = name.replacingOccurrences(of: "（闰）", with: "")
        return termJu[key] ?? (true, [1, 7, 4])
    }

    static func fuTou(for day: StemBranch) -> StemBranch {
        var idx = day.sexagenaryIndex
        for _ in 0..<10 {
            let sb = StemBranch.from(sexagenaryIndex: idx)
            if sb.stem == .jia || sb.stem == .ji { return sb }
            idx = (idx + 59) % 60
        }
        return day
    }

    static func yuanIndex(fuTou: StemBranch) -> Int {
        switch fuTou.branch {
        case .zi, .wu, .mao, .you: return 0
        case .yin, .shen, .si, .hai: return 1
        case .chen, .xu, .chou, .wei: return 2
        }
    }

    static func yuanName(_ index: Int) -> String {
        ["上元", "中元", "下元"][max(0, min(2, index))]
    }

    /// 统一入口
    static func resolve(
        day: StemBranch,
        queryDate: Date,
        timeZone: TimeZone,
        method: JuMethod
    ) -> JuResolution {
        switch method {
        case .chaiBu:
            let term = SolarTerms.currentTerm(for: queryDate, timeZone: timeZone)
            var r = resolveChaibu(day: day, solarTermName: term.name)
            r.juMethod = .chaiBu
            r.solarTermInstant = term.approximateDate
            r.phase = .zhengShou
            r.isRunQi = false
            return r
        case .zhiYun:
            return ZhiYunResolver.resolve(queryDate: queryDate, day: day, timeZone: timeZone)
        }
    }

    /// 拆补：当前已交节气 + 符头三元
    static func resolveChaibu(day: StemBranch, solarTermName: String) -> JuResolution {
        let ft = fuTou(for: day)
        let yi = yuanIndex(fuTou: ft)
        let entry = termJuEntry(solarTermName)
        return JuResolution(
            isYangDun: entry.isYang,
            juNumber: entry.jus[yi],
            yuanName: yuanName(yi),
            yuanIndex: yi,
            solarTermName: solarTermName,
            fuTou: ft,
            juMethod: .chaiBu,
            phase: .zhengShou,
            isRunQi: false,
            solarTermInstant: nil
        )
    }

    /// 兼容旧测试 API
    static func resolve(day: StemBranch, solarTermName: String) -> JuResolution {
        resolveChaibu(day: day, solarTermName: solarTermName)
    }

    static func ju(term: String, yuanIndex: Int) -> (isYang: Bool, ju: Int)? {
        guard let e = termJu[term], (0...2).contains(yuanIndex) else { return nil }
        return (e.isYang, e.jus[yuanIndex])
    }
}
