//
//  InterpretationEngine.swift
//  QimenDunjia
//
//  最小可用解读模板（非断事承诺）。
//

import Foundation

enum InterpretationEngine {

    static func build(
        isYang: Bool,
        ju: Int,
        zhiFu: NineStar,
        zhiShi: EightGate,
        zhiFuPalace: Palace,
        zhiShiPalace: Palace,
        cells: [PalaceCell],
        hour: StemBranch,
        xunKong: [EarthlyBranch]
    ) -> [InterpretationItem] {
        var items: [InterpretationItem] = []

        items.append(InterpretationItem(
            title: "局象",
            detail: "\(isYang ? "阳遁" : "阴遁")\(ju)局。值符\(zhiFu.name)在\(zhiFuPalace.name)宫，值使\(zhiShi.displayName)在\(zhiShiPalace.name)宫。仅供学习参考。",
            tone: .neutral
        ))

        if let cell = cells.first(where: { $0.palace == zhiFuPalace }),
           cell.star?.innatePalace == zhiFuPalace {
            items.append(InterpretationItem(
                title: "伏吟",
                detail: "值符星归本位，事易重复胶着，宜守。",
                tone: .caution
            ))
        }

        if zhiShi.isAuspicious {
            items.append(InterpretationItem(
                title: "值使较顺",
                detail: "值使\(zhiShi.displayName)，传统多主可推进（仍看用神）。",
                tone: .auspicious
            ))
        } else {
            items.append(InterpretationItem(
                title: "值使需慎",
                detail: "值使\(zhiShi.displayName)，传统多主阻滞或变动。",
                tone: .caution
            ))
        }

        let kong = xunKong.map(\.name).joined(separator: "、")
        items.append(InterpretationItem(
            title: "旬空",
            detail: "\(hour.name)时旬空：\(kong)。",
            tone: .neutral
        ))

        return items
    }
}
