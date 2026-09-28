//
//  BaZiEngine.swift
//  BaZiYuCe
//

import Foundation

enum BaZiEngine {

    static func chart(
        birthDate: Date,
        gender: Gender,
        timeZone: TimeZone = .current
    ) -> BaZiChart {
        let pillars = GanzhiCalendar.stemBranchFourPillars(for: birthDate, timeZone: timeZone)
        let dayMaster = pillars.day.stem

        let details: [PillarDetail] = [
            PillarDetail(
                kind: .year,
                stemBranch: pillars.year,
                stemGod: TenGods.of(stem: pillars.year.stem, dayMaster: dayMaster),
                branchMainGod: TenGods.ofBranchMain(branch: pillars.year.branch, dayMaster: dayMaster)
            ),
            PillarDetail(
                kind: .month,
                stemBranch: pillars.month,
                stemGod: TenGods.of(stem: pillars.month.stem, dayMaster: dayMaster),
                branchMainGod: TenGods.ofBranchMain(branch: pillars.month.branch, dayMaster: dayMaster)
            ),
            PillarDetail(
                kind: .day,
                stemBranch: pillars.day,
                stemGod: nil, // 日主
                branchMainGod: TenGods.ofBranchMain(branch: pillars.day.branch, dayMaster: dayMaster)
            ),
            PillarDetail(
                kind: .hour,
                stemBranch: pillars.hour,
                stemGod: TenGods.of(stem: pillars.hour.stem, dayMaster: dayMaster),
                branchMainGod: TenGods.ofBranchMain(branch: pillars.hour.branch, dayMaster: dayMaster)
            )
        ]

        let balance = FiveElements.balance(
            year: pillars.year,
            month: pillars.month,
            day: pillars.day,
            hour: pillars.hour
        )

        return BaZiChart(
            id: UUID(),
            birthDate: birthDate,
            gender: gender,
            timeZoneIdentifier: timeZone.identifier,
            year: pillars.year,
            month: pillars.month,
            day: pillars.day,
            hour: pillars.hour,
            pillars: details,
            balance: balance,
            solarTermNote: pillars.note
        )
    }
}
