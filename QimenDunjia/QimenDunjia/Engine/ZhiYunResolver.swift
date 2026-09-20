//
//  ZhiYunResolver.swift
//  QimenDunjia
//
//  时家奇门 · 置闰法定局（可选口径；默认仍为拆补）。
//
//  规则来源（交叉对照，择一可实现口径并注明歧义）：
//  - 《易学象数论·超神接气直指》：正授→超神→闰奇→接气循环；置闰仅芒种、大雪；超约九日置闰
//  - 奇门派 / 遁甲演义口述：符头甲己，子午卯酉上元；超神符先节后；接气节先符后
//
//  SOLID（本实现）：
//  1. 以「上元符头」（甲子/甲午/己卯/己酉）起每节气三元（上中下各 5 日）
//  2. 自某冬至起顺序分配符头段；芒种/大雪若上元符头早于交节 ≥9 日，则于本气三元后再重复三元为「闰奇」
//  3. 局数歌与拆补相同（芒种六三九、大雪四七一…）
//
//  APPROXIMATE / AMBIGUITY：
//  - 「超九日」阈值取 ≥9 日（有的本子写九至十一日）
//  - 起始冬至与上元符头对齐：取交节前最近上元符头（超神/正授），若无则取交节后第一上元符头（接气）
//  - 与部分置闰软件在极端超神年可能差一元；重要用事请交叉验证
//

import Foundation

enum ZhiYunPhase: String, Codable, Hashable {
    case zhengShou = "正授"
    case chaoShen = "超神"
    case runQi = "闰奇"
    case jieQi = "接气"
}

struct ZhiYunYuanSlot: Hashable {
    let termName: String
    let yuanIndex: Int
    let start: Date // inclusive, start of day UTC-ish absolute
    let end: Date   // exclusive
    let isRunQi: Bool
    let termInstant: Date
}

enum ZhiYunResolver {

    /// 上元符头：甲子、甲午、己卯、己酉
    static func isShangYuanFuTou(_ sb: StemBranch) -> Bool {
        guard sb.stem == .jia || sb.stem == .ji else { return false }
        switch sb.branch {
        case .zi, .wu, .mao, .you: return true
        default: return false
        }
    }

    static func resolve(
        queryDate: Date,
        day: StemBranch,
        timeZone: TimeZone
    ) -> JuResolution {
        let slots = buildSchedule(around: queryDate, timeZone: timeZone)
        let dayStart = startOfDay(queryDate, timeZone: timeZone)
        guard let slot = slots.last(where: { $0.start <= dayStart && dayStart < $0.end })
                ?? slots.first(where: { $0.start <= dayStart })
                ?? slots.last
        else {
            // Fallback: behave like chaibu on current term
            let term = SolarTerms.currentTerm(for: queryDate, timeZone: timeZone)
            var r = JuResolver.resolveChaibu(day: day, solarTermName: term.name)
            r = JuResolution(
                isYangDun: r.isYangDun,
                juNumber: r.juNumber,
                yuanName: r.yuanName,
                yuanIndex: r.yuanIndex,
                solarTermName: r.solarTermName,
                fuTou: r.fuTou,
                juMethod: .zhiYun,
                phase: .chaoShen,
                isRunQi: false,
                solarTermInstant: term.approximateDate
            )
            return r
        }

        let ft = JuResolver.fuTou(for: day)
        let entry = JuResolver.termJuEntry(slot.termName)
        let ju = entry.jus[slot.yuanIndex]
        let phase = classifyPhase(
            shangStart: slotsShangStart(for: slot, in: slots),
            termInstant: slot.termInstant,
            isRunQi: slot.isRunQi
        )
        return JuResolution(
            isYangDun: entry.isYang,
            juNumber: ju,
            yuanName: JuResolver.yuanName(slot.yuanIndex),
            yuanIndex: slot.yuanIndex,
            solarTermName: slot.termName + (slot.isRunQi ? "（闰）" : ""),
            fuTou: ft,
            juMethod: .zhiYun,
            phase: phase,
            isRunQi: slot.isRunQi,
            solarTermInstant: slot.termInstant
        )
    }

    // MARK: - Schedule

    static func buildSchedule(around date: Date, timeZone: TimeZone) -> [ZhiYunYuanSlot] {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        let year = cal.component(.year, from: date)

        // Terms from year-1 … year+1
        var terms: [SolarTermInfo] = []
        for y in (year - 1)...(year + 1) {
            terms.append(contentsOf: SolarTerms.terms(forSolarYear: y))
        }
        terms.sort { $0.approximateDate < $1.approximateDate }

        // All 甲己 days (yuan heads) in range
        let rangeStart = cal.date(from: DateComponents(year: year - 1, month: 1, day: 1))!
        let rangeEnd = cal.date(from: DateComponents(year: year + 2, month: 1, day: 1))!
        let yuanHeads = allJiaJiDays(from: rangeStart, to: rangeEnd, timeZone: timeZone)

        // Start at first 冬至 in list
        guard let dongZhiIdx = terms.firstIndex(where: { $0.name == "冬至" }) else { return [] }
        let t0 = terms[dongZhiIdx]
        let shangHeads = yuanHeads.filter { isShangYuanFuTou(dayStemBranch(on: $0, timeZone: timeZone)) }

        // Align first 上元符头 for this 冬至
        guard var yuanHeadIdx = indexOfAlignedShangFuTou(
            termInstant: t0.approximateDate,
            shangHeads: shangHeads,
            timeZone: timeZone
        ) else { return [] }

        // Map shangHeads index → position in yuanHeads
        func yuanHeadsIndex(ofShang shangIdx: Int) -> Int? {
            let d = shangHeads[shangIdx]
            return yuanHeads.firstIndex(of: d)
        }

        guard var cursor = yuanHeadsIndex(ofShang: yuanHeadIdx) else { return [] }

        var slots: [ZhiYunYuanSlot] = []
        for tIdx in dongZhiIdx..<terms.count {
            let term = terms[tIdx]
            // Need 3 yuan heads for 上中下
            guard cursor + 2 < yuanHeads.count else { break }

            let shangStart = yuanHeads[cursor]
            for y in 0..<3 {
                let start = yuanHeads[cursor + y]
                let end: Date
                if cursor + y + 1 < yuanHeads.count {
                    end = yuanHeads[cursor + y + 1]
                } else {
                    end = cal.date(byAdding: .day, value: 5, to: start) ?? start.addingTimeInterval(5 * 86400)
                }
                slots.append(ZhiYunYuanSlot(
                    termName: term.name,
                    yuanIndex: y,
                    start: startOfDay(start, timeZone: timeZone),
                    end: startOfDay(end, timeZone: timeZone),
                    isRunQi: false,
                    termInstant: term.approximateDate
                ))
            }
            cursor += 3

            // 置闰：仅芒种、大雪；上元符头早于交节 ≥ 9 日
            if term.name == "芒种" || term.name == "大雪" {
                let chaoDays = calendarDays(
                    from: startOfDay(shangStart, timeZone: timeZone),
                    to: term.approximateDate,
                    timeZone: timeZone
                )
                // 符头在节前且超 ≥ 9
                if shangStart <= term.approximateDate && chaoDays >= 9 {
                    guard cursor + 2 < yuanHeads.count else { break }
                    for y in 0..<3 {
                        let start = yuanHeads[cursor + y]
                        let end: Date
                        if cursor + y + 1 < yuanHeads.count {
                            end = yuanHeads[cursor + y + 1]
                        } else {
                            end = cal.date(byAdding: .day, value: 5, to: start) ?? start.addingTimeInterval(5 * 86400)
                        }
                        slots.append(ZhiYunYuanSlot(
                            termName: term.name,
                            yuanIndex: y,
                            start: startOfDay(start, timeZone: timeZone),
                            end: startOfDay(end, timeZone: timeZone),
                            isRunQi: true,
                            termInstant: term.approximateDate
                        ))
                    }
                    cursor += 3
                }
            }

            _ = yuanHeadIdx
        }
        return slots
    }

    /// 交节前最近上元符头；若无（接气），取交节后第一个上元符头。
    static func indexOfAlignedShangFuTou(
        termInstant: Date,
        shangHeads: [Date],
        timeZone: TimeZone
    ) -> Int? {
        let termDay = startOfDay(termInstant, timeZone: timeZone)
        var bestBefore: Int?
        for (i, d) in shangHeads.enumerated() {
            let sd = startOfDay(d, timeZone: timeZone)
            if sd <= termDay { bestBefore = i }
            else { break }
        }
        if let b = bestBefore { return b }
        return shangHeads.firstIndex { startOfDay($0, timeZone: timeZone) > termDay }
    }

    private static func slotsShangStart(for slot: ZhiYunYuanSlot, in slots: [ZhiYunYuanSlot]) -> Date {
        // Find 上元 start of the same term block (same isRunQi flag, contiguous)
        if slot.yuanIndex == 0 { return slot.start }
        if let idx = slots.firstIndex(where: { $0.start == slot.start }) {
            let shangIdx = idx - slot.yuanIndex
            if shangIdx >= 0 { return slots[shangIdx].start }
        }
        return slot.start
    }

    private static func classifyPhase(shangStart: Date, termInstant: Date, isRunQi: Bool) -> ZhiYunPhase {
        if isRunQi { return .runQi }
        let s = shangStart.timeIntervalSince1970
        let t = termInstant.timeIntervalSince1970
        let dayDiff = abs(s - t) / 86400
        if dayDiff < 1.0 { return .zhengShou }
        if s < t { return .chaoShen }
        return .jieQi
    }

    // MARK: - Calendar helpers

    static func startOfDay(_ date: Date, timeZone: TimeZone) -> Date {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        return cal.startOfDay(for: date)
    }

    static func calendarDays(from: Date, to: Date, timeZone: TimeZone) -> Int {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        return cal.dateComponents([.day], from: startOfDay(from, timeZone: timeZone), to: to).day ?? 0
    }

    static func allJiaJiDays(from: Date, to: Date, timeZone: TimeZone) -> [Date] {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        var result: [Date] = []
        var d = startOfDay(from, timeZone: timeZone)
        let end = startOfDay(to, timeZone: timeZone)
        // Find first 甲/己 by scanning
        while d < end {
            let sb = dayStemBranch(on: d, timeZone: timeZone)
            if sb.stem == .jia || sb.stem == .ji {
                result.append(d)
            }
            guard let next = cal.date(byAdding: .day, value: 1, to: d) else { break }
            d = next
        }
        return result
    }

    static func dayStemBranch(on dayStart: Date, timeZone: TimeZone) -> StemBranch {
        // Noon on that civil day to avoid DST edge; hour=12 → not 子时换日
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        let c = cal.dateComponents([.year, .month, .day], from: dayStart)
        return GanzhiCalendar.dayStemBranch(
            year: c.year ?? 2000,
            month: c.month ?? 1,
            day: c.day ?? 1,
            hour: 12,
            timeZone: timeZone
        )
    }
}
