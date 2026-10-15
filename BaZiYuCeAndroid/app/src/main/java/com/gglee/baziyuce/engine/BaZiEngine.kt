package com.gglee.baziyuce.engine

import java.util.Date
import java.util.TimeZone

object BaZiEngine {

    fun chart(
        birthDate: Date,
        gender: Gender,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): BaZiChart {
        val pillars = GanzhiCalendar.stemBranchFourPillars(birthDate, timeZone)
        val dayMaster = pillars.day.stem

        val details = listOf(
            PillarDetail(
                kind = PillarKind.YEAR,
                stemBranch = pillars.year,
                stemGod = TenGods.of(pillars.year.stem, dayMaster),
                branchMainGod = TenGods.ofBranchMain(pillars.year.branch, dayMaster),
            ),
            PillarDetail(
                kind = PillarKind.MONTH,
                stemBranch = pillars.month,
                stemGod = TenGods.of(pillars.month.stem, dayMaster),
                branchMainGod = TenGods.ofBranchMain(pillars.month.branch, dayMaster),
            ),
            PillarDetail(
                kind = PillarKind.DAY,
                stemBranch = pillars.day,
                stemGod = null,
                branchMainGod = TenGods.ofBranchMain(pillars.day.branch, dayMaster),
            ),
            PillarDetail(
                kind = PillarKind.HOUR,
                stemBranch = pillars.hour,
                stemGod = TenGods.of(pillars.hour.stem, dayMaster),
                branchMainGod = TenGods.ofBranchMain(pillars.hour.branch, dayMaster),
            ),
        )

        val balance = FiveElements.balance(
            year = pillars.year,
            month = pillars.month,
            day = pillars.day,
            hour = pillars.hour,
        )

        return BaZiChart(
            birthDate = birthDate,
            gender = gender,
            timeZoneIdentifier = timeZone.id,
            year = pillars.year,
            month = pillars.month,
            day = pillars.day,
            hour = pillars.hour,
            pillars = details,
            balance = balance,
            solarTermNote = pillars.note,
        )
    }
}
