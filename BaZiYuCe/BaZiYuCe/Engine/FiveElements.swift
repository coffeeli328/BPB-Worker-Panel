//
//  FiveElements.swift
//  BaZiYuCe
//
//  简易五行力量：天干满分 + 地支藏干加权。
//

import Foundation

enum FiveElements {

    /// 藏干权重：主气 / 中气 / 余气
    private static let hiddenWeights: [Double] = [1.0, 0.5, 0.25]

    static func balance(year: StemBranch, month: StemBranch, day: StemBranch, hour: StemBranch) -> FiveElementBalance {
        var scores: [WuXing: Double] = Dictionary(uniqueKeysWithValues: WuXing.allCases.map { ($0, 0.0) })

        func addStem(_ stem: HeavenlyStem, weight: Double) {
            scores[stem.wuXing, default: 0] += weight
        }

        func addBranch(_ branch: EarthlyBranch, monthBoost: Double) {
            let stems = branch.hiddenStems
            for (i, s) in stems.enumerated() {
                let w = (i < hiddenWeights.count ? hiddenWeights[i] : 0.2) * monthBoost
                addStem(s, weight: w)
            }
        }

        // 月令当令加分
        let monthElement = month.branch.wuXing
        for pillar in [year, month, day, hour] {
            addStem(pillar.stem, weight: 1.2)
            let boost = pillar.branch.wuXing == monthElement ? 1.35 : 1.0
            addBranch(pillar.branch, monthBoost: boost)
        }
        // 再给月支本身额外权重
        scores[monthElement, default: 0] += 0.8

        return FiveElementBalance(scores: scores)
    }
}
