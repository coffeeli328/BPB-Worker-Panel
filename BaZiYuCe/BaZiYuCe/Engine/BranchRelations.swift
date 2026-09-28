//
//  BranchRelations.swift
//  BaZiYuCe
//
//  地支刑冲合害简表（规则透明、可扩展）。
//

import Foundation

enum BranchRelations {

    static func describe(flowing: EarthlyBranch, natal: [EarthlyBranch]) -> [String] {
        var notes: [String] = []
        for b in natal {
            if flowing.clash == b {
                notes.append("\(flowing.name)冲\(b.name)")
            }
            if flowing.sixHarmony == b {
                notes.append("\(flowing.name)合\(b.name)")
            }
            if isHarm(flowing, b) {
                notes.append("\(flowing.name)害\(b.name)")
            }
            if isXing(flowing, b) {
                notes.append("\(flowing.name)刑\(b.name)")
            }
        }
        // 去重保序
        var seen = Set<String>()
        return notes.filter { seen.insert($0).inserted }
    }

    private static func isHarm(_ a: EarthlyBranch, _ b: EarthlyBranch) -> Bool {
        // 六害：子未、丑午、寅巳、卯辰、申亥、酉戌
        let pairs: Set<Set<Int>> = [
            [0, 7], [1, 6], [2, 5], [3, 4], [8, 11], [9, 10]
        ]
        return pairs.contains([a.rawValue, b.rawValue])
    }

    private static func isXing(_ a: EarthlyBranch, _ b: EarthlyBranch) -> Bool {
        if a == b {
            // 自刑：辰辰、午午、酉酉、亥亥
            return [4, 6, 9, 11].contains(a.rawValue)
        }
        let triples: [[Int]] = [
            [2, 5, 8],   // 寅巳申
            [1, 7, 10],  // 丑未戌
            [0, 3]       // 子卯
        ]
        for t in triples {
            if t.contains(a.rawValue) && t.contains(b.rawValue) { return true }
        }
        return false
    }
}
