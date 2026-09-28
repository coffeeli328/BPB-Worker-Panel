//
//  TenGods.swift
//  BaZiYuCe
//
//  以日主为基准的十神推导。
//

import Foundation

enum TenGods {

    static func of(stem: HeavenlyStem, dayMaster: HeavenlyStem) -> TenGod {
        let me = dayMaster.wuXing
        let other = stem.wuXing
        let samePolarity = stem.yinYang == dayMaster.yinYang

        if other == me {
            return samePolarity ? .biJian : .jieCai
        }
        if other == me.generates {
            return samePolarity ? .shiShen : .shangGuan
        }
        if other == me.controls {
            return samePolarity ? .pianCai : .zhengCai
        }
        if other == me.controlledBy {
            return samePolarity ? .qiSha : .zhengGuan
        }
        // other == me.generatedBy
        return samePolarity ? .pianYin : .zhengYin
    }

    static func ofBranchMain(branch: EarthlyBranch, dayMaster: HeavenlyStem) -> TenGod {
        of(stem: branch.hiddenStems[0], dayMaster: dayMaster)
    }
}
