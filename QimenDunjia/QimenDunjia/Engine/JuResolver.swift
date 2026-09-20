//
//  JuResolver.swift
//  QimenDunjia
//
//  拆补法定局：符头定三元 + 节气定局数。
//  口径来源（交叉对照）：
//  - 主流时家奇门「转盘排宫 + 拆补定局」（如 DestinySeek 公开算法说明）
//  - 常见定局歌「冬至惊蛰一七四…」
//  CAVEAT: 不实现置闰法/超神接气；与置闰派软件局数可能不同。
//

import Foundation

struct JuResolution: Hashable {
    let isYangDun: Bool
    let juNumber: Int
    let yuanName: String
    let yuanIndex: Int // 0上 1中 2下
    let solarTermName: String
    let fuTou: StemBranch
}

enum JuResolver {

    /// 节气 → (阳?, 上中下元局数)
    /// 歌诀（阳遁）：冬至惊蛰一七四；小寒二八五；大寒春分三九六；
    /// 立春八五二；雨水九六三；清明立夏四一七；谷雨小满五二八；芒种六三九
    /// 歌诀（阴遁）：夏至白露九三六；小暑八二五；大暑秋分七一四；
    /// 立秋二五八；处暑一四七；寒露立冬六九三；霜降小雪五八二；大雪四七一
    private static let termJu: [String: (isYang: Bool, jus: [Int])] = [
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

    /// 由日柱找符头（向前取甲/己日），再定上中下元。
    /// SOLID: 甲己为符头；子午卯酉上元、寅申巳亥中元、辰戌丑未下元。
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

    static func resolve(day: StemBranch, solarTermName: String) -> JuResolution {
        let ft = fuTou(for: day)
        let yi = yuanIndex(fuTou: ft)
        let entry = termJu[solarTermName] ?? (true, [1, 7, 4])
        let ju = entry.jus[yi]
        return JuResolution(
            isYangDun: entry.isYang,
            juNumber: ju,
            yuanName: yuanName(yi),
            yuanIndex: yi,
            solarTermName: solarTermName,
            fuTou: ft
        )
    }

    /// 仅局数表查询（供测试）
    static func ju(term: String, yuanIndex: Int) -> (isYang: Bool, ju: Int)? {
        guard let e = termJu[term], (0...2).contains(yuanIndex) else { return nil }
        return (e.isYang, e.jus[yuanIndex])
    }
}
