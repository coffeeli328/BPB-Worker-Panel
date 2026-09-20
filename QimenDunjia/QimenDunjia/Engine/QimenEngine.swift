//
//  QimenEngine.swift
//  QimenDunjia
//
//  时家奇门 · 转盘排宫法 · 拆补法定局
//
//  SOLID 规则（已按经典教材/主流软件口径实现，并用黄金用例锁定）：
//  1. 地盘：戊己庚辛壬癸丁丙乙，阳顺阴逆，戊起于局数宫
//  2. 旬首六甲隐仪：甲子戊…甲寅癸 → 仪落地盘宫之先天星/门 = 值符/值使
//  3. 值符随时干（甲用隐仪）；落中寄坤二；九星按 clockRing 顺时针整体旋转；禽随芮
//  4. 值使随时支：自旬首宫起按阳顺阴逆飞九宫（含中）走时辰序；八门按 clockRing 顺时针旋转
//  5. 八神：自值符落宫起，阳顺(clock) / 阴逆，白虎↔勾陈、玄武↔朱雀随阴阳遁换名
//  6. 旬空：时辰所在旬之空亡地支
//
//  APPROXIMATE / AMBIGUITY：
//  - 节气：Meeus 低精度 + ΔT，1900–2100 残差通常数分钟（见 SolarTerms / AstronomyCore）
//  - 真太阳时：经度+均时差；未计大气折射（见 TrueSolarTime）
//  - 中五寄宫取坤二（常见现代口径）；部分流派阳遁寄艮
//  - 年/月柱仅作展示，不影响时家局盘
//

import Foundation

enum QimenEngine {

    /// 六甲旬首 → 隐干（仪）
    static let xunShouYi: [(start: Int, stem: HeavenlyStem)] = [
        (0, .wu),   // 甲子戊
        (10, .ji),  // 甲戌己
        (20, .geng),// 甲申庚
        (30, .xin), // 甲午辛
        (40, .ren), // 甲辰壬
        (50, .gui)  // 甲寅癸
    ]

    static let qiYiOrder: [HeavenlyStem] = [.wu, .ji, .geng, .xin, .ren, .gui, .ding, .bing, .yi]

    // MARK: - Public

    static func generate(request: ChartRequest) -> QimenChart {
        let tz = resolvedTimeZone(request)
        let civil = request.date

        // 真太阳时：仅用于干支四柱（尤其时辰）；节气仍用绝对民用瞬间
        let solarAdj: (date: Date, longitudeMinutes: Double, eotMinutes: Double, totalMinutes: Double)
        if request.useTrueSolarTime {
            solarAdj = TrueSolarTime.adjustedDate(
                civil: civil,
                timeZone: tz,
                longitudeEastDegrees: request.longitude,
                applyEquationOfTime: true
            )
        } else {
            solarAdj = (civil, 0, 0, 0)
        }

        let pillars = GanzhiCalendar.stemBranchFourPillars(for: solarAdj.date, timeZone: tz)
        // 定局：拆补用「已交节气」；置闰用符头超神接气日程
        let juRes = JuResolver.resolve(
            day: pillars.day,
            queryDate: civil,
            timeZone: tz,
            method: request.juMethod
        )
        let termInstant = juRes.solarTermInstant
            ?? SolarTerms.currentTerm(for: civil, timeZone: tz).approximateDate

        let plate = buildPlate(
            isYangDun: juRes.isYangDun,
            juNumber: juRes.juNumber,
            hour: pillars.hour
        )

        let cells = Palace.allCases.map { p -> PalaceCell in
            PalaceCell(
                palace: p,
                earthStem: plate.earth[p],
                heavenStem: plate.heaven[p],
                star: plate.stars[p],
                gate: plate.gates[p],
                deity: plate.deities[p],
                isEmpty: plate.xunKongPalaces.contains(p),
                isZhiFu: p == plate.zhiFuPalace,
                isZhiShi: p == plate.zhiShiPalace
            )
        }

        let question = request.question.trimmingCharacters(in: .whitespacesAndNewlines)
        let topic = QuestionTopic.detect(from: question)
        let interpretations = InterpretationEngine.build(
            question: question,
            isYang: juRes.isYangDun,
            ju: juRes.juNumber,
            zhiFu: plate.zhiFuStar,
            zhiShi: plate.zhiShiGate,
            zhiFuPalace: plate.zhiFuPalace,
            zhiShiPalace: plate.zhiShiPalace,
            cells: cells,
            hour: pillars.hour,
            xunKong: plate.xunKongBranches,
            isYangDun: juRes.isYangDun
        )

        return QimenChart(
            id: UUID(),
            createdAt: Date(),
            queryDate: civil,
            trueSolarDate: solarAdj.date,
            calendarMode: request.calendarMode,
            timeZoneIdentifier: tz.identifier,
            method: request.method,
            locationNote: request.locationNote,
            longitude: request.longitude,
            usedTrueSolarTime: request.useTrueSolarTime,
            longitudeCorrectionMinutes: solarAdj.longitudeMinutes,
            equationOfTimeMinutes: solarAdj.eotMinutes,
            yearSB: pillars.year,
            monthSB: pillars.month,
            daySB: pillars.day,
            hourSB: pillars.hour,
            isYangDun: juRes.isYangDun,
            juNumber: juRes.juNumber,
            solarTermName: juRes.solarTermName,
            yuanName: juRes.yuanName,
            juMethod: juRes.juMethod,
            juPhase: juRes.phase,
            isRunQi: juRes.isRunQi,
            solarTermInstant: termInstant,
            zhiFuStar: plate.zhiFuStar,
            zhiShiGate: plate.zhiShiGate,
            zhiFuPalace: plate.zhiFuPalace,
            zhiShiPalace: plate.zhiShiPalace,
            xunKong: plate.xunKongBranches,
            cells: cells,
            question: question,
            questionTopic: topic,
            interpretations: interpretations
        )
    }

    /// 纯排盘（给定阴阳遁局数与时柱）— 供黄金用例测试，不依赖历法。
    static func buildPlate(
        isYangDun: Bool,
        juNumber: Int,
        hour: StemBranch
    ) -> PlateResult {
        let earth = earthPlate(isYangDun: isYangDun, ju: juNumber)
        let (xunStart, yiStem) = xunShou(for: hour)
        let xunKong = xunKongBranches(xunStart: xunStart)

        // 旬首仪落地盘宫 → 值符星、值使门
        let xunShouPalace = earth.first { $0.value == yiStem }?.key ?? .kan1
        let zhiFuStar = NineStar.from(palace: xunShouPalace)
        let zhiShiGate: EightGate = {
            if xunShouPalace == .zhong5 {
                // AMBIGUITY: 中五无门；常见取死门（坤）或寄宫门。取寄坤之死门。
                return .si
            }
            return EightGate.innate(at: xunShouPalace) ?? .xiu
        }()

        // 时干落宫（甲→旬首仪）
        let hourStem = hour.stem == .jia ? yiStem : hour.stem
        var hourStemPalace = earth.first { $0.value == hourStem }?.key ?? .kan1
        let hourStemOnZhong = hourStemPalace == .zhong5
        if hourStemOnZhong { hourStemPalace = Palace.zhongHost }

        // 值符随时干
        let zhiFuPalace = hourStemPalace
        let stars = rotateStars(zhiFu: zhiFuStar, to: zhiFuPalace)
        let heaven = rotateHeaven(earth: earth, stars: stars)

        // 值使随时支：自旬首宫飞九宫（含中）；落中则门布寄坤二
        let zhiShiRaw = moveZhiShiPalace(
            from: xunShouPalace,
            hourOffset: hour.sexagenaryIndex - xunStart,
            isYangDun: isYangDun
        )
        let zhiShiDisplay = zhiShiRaw == .zhong5 ? Palace.zhongHost : zhiShiRaw
        let gatesFinal = rotateGates(zhiShi: zhiShiGate, to: zhiShiDisplay)

        let deities = arrangeDeities(start: zhiFuPalace, isYangDun: isYangDun)
        let kongPalaces = xunKongPalaces(branches: xunKong)

        return PlateResult(
            earth: earth,
            heaven: heaven,
            stars: stars,
            gates: gatesFinal,
            deities: deities,
            zhiFuStar: zhiFuStar,
            zhiShiGate: zhiShiGate,
            zhiFuPalace: zhiFuPalace,
            zhiShiPalace: zhiShiDisplay,
            xunKongBranches: xunKong,
            xunKongPalaces: kongPalaces,
            xunShouPalace: xunShouPalace,
            yiStem: yiStem,
            hourStemOnZhong: hourStemOnZhong
        )
    }

    struct PlateResult {
        let earth: [Palace: HeavenlyStem]
        let heaven: [Palace: HeavenlyStem]
        let stars: [Palace: NineStar]
        let gates: [Palace: EightGate]
        let deities: [Palace: EightDeity]
        let zhiFuStar: NineStar
        let zhiShiGate: EightGate
        let zhiFuPalace: Palace
        let zhiShiPalace: Palace
        let xunKongBranches: [EarthlyBranch]
        let xunKongPalaces: Set<Palace>
        let xunShouPalace: Palace
        let yiStem: HeavenlyStem
        let hourStemOnZhong: Bool
    }

    // MARK: - 地盘

    static func earthPlate(isYangDun: Bool, ju: Int) -> [Palace: HeavenlyStem] {
        var map: [Palace: HeavenlyStem] = [:]
        let order = isYangDun ? Palace.yangFly : Palace.yinFly
        let start = Palace(rawValue: ju) ?? .kan1
        guard let sIdx = order.firstIndex(of: start) else { return map }
        for (i, stem) in qiYiOrder.enumerated() {
            map[order[(sIdx + i) % 9]] = stem
        }
        return map
    }

    // MARK: - 旬首 / 旬空

    static func xunShou(for hour: StemBranch) -> (start: Int, yi: HeavenlyStem) {
        let idx = hour.sexagenaryIndex
        let start = (idx / 10) * 10
        let yi = xunShouYi.first { $0.start == start }?.stem ?? .wu
        return (start, yi)
    }

    static func xunKongBranches(xunStart: Int) -> [EarthlyBranch] {
        switch xunStart {
        case 0: return [.xu, .hai]
        case 10: return [.shen, .you]
        case 20: return [.wu, .wei]
        case 30: return [.chen, .si]
        case 40: return [.yin, .mao]
        default: return [.zi, .chou]
        }
    }

    /// 宫位对应地支（四正四维简化）：用于旬空标宫
    static func xunKongPalaces(branches: [EarthlyBranch]) -> Set<Palace> {
        var set = Set<Palace>()
        let map: [EarthlyBranch: Palace] = [
            .zi: .kan1, .chou: .gen8, .yin: .gen8, .mao: .zhen3,
            .chen: .xun4, .si: .xun4, .wu: .li9, .wei: .kun2,
            .shen: .kun2, .you: .dui7, .xu: .qian6, .hai: .qian6
        ]
        // SOLID for 戌亥空→乾: 用宫支对应；丑未兼寄
        for b in branches {
            if let p = map[b] { set.insert(p) }
        }
        return set
    }

    // MARK: - 九星转盘

    static func rotateStars(zhiFu: NineStar, to target: Palace) -> [Palace: NineStar] {
        let ring = Palace.clockRing
        let starRing = NineStar.clockStars
        // 值符若为天禽，按天芮位旋转，禽仍随芮（中宫标禽）
        let pivotStar: NineStar = (zhiFu == .qin) ? .rui : zhiFu
        let toPalace = target == .zhong5 ? Palace.zhongHost : target
        guard let fromIdx = starRing.firstIndex(of: pivotStar),
              let toIdx = ring.firstIndex(of: toPalace) else {
            return [:]
        }
        let shift = (toIdx - fromIdx + 8) % 8
        var map: [Palace: NineStar] = [:]
        for i in 0..<8 {
            map[ring[i]] = starRing[(i - shift + 8) % 8]
        }
        map[.zhong5] = .qin
        return map
    }

    static func rotateHeaven(earth: [Palace: HeavenlyStem], stars: [Palace: NineStar]) -> [Palace: HeavenlyStem] {
        // 天盘干 = 随先天星迁移的地盘干；中五干随禽/芮
        var heaven: [Palace: HeavenlyStem] = [:]
        for src in Palace.allCases {
            guard let stem = earth[src] else { continue }
            let innate = NineStar.from(palace: src)
            if innate == .qin {
                // 中五干随芮所在
                if let dest = stars.first(where: { $0.value == .rui })?.key {
                    heaven[dest] = stem
                }
                continue
            }
            if let dest = stars.first(where: { $0.value == innate })?.key {
                heaven[dest] = stem
            }
        }
        // 中宫天盘：若无则留空用地盘
        if heaven[.zhong5] == nil { heaven[.zhong5] = earth[.zhong5] }
        for p in Palace.allCases where heaven[p] == nil {
            heaven[p] = earth[p]
        }
        return heaven
    }

    // MARK: - 八门

    static func moveZhiShiPalace(from xunShouPalace: Palace, hourOffset: Int, isYangDun: Bool) -> Palace {
        let fly = isYangDun ? Palace.yangFly : Palace.yinFly
        guard let sIdx = fly.firstIndex(of: xunShouPalace) else { return xunShouPalace }
        let steps = ((hourOffset % 9) + 9) % 9
        // 癸为第 10 个时辰（offset 9）回到本宫：9 % 9 = 0 ✓
        return fly[(sIdx + steps) % 9]
    }

    static func rotateGates(zhiShi: EightGate, to target: Palace) -> [Palace: EightGate] {
        let ring = Palace.clockRing
        let gates = EightGate.clockGates
        let dest = target == .zhong5 ? Palace.zhongHost : target
        guard let fromIdx = gates.firstIndex(of: zhiShi),
              let toIdx = ring.firstIndex(of: dest) else { return [:] }
        let shift = (toIdx - fromIdx + 8) % 8
        var map: [Palace: EightGate] = [:]
        for i in 0..<8 {
            map[ring[i]] = gates[(i - shift + 8) % 8]
        }
        return map
    }

    // MARK: - 八神

    static func arrangeDeities(start: Palace, isYangDun: Bool) -> [Palace: EightDeity] {
        let ring = Palace.clockRing
        let dest = start == .zhong5 ? Palace.zhongHost : start
        guard let sIdx = ring.firstIndex(of: dest) else { return [:] }
        var map: [Palace: EightDeity] = [:]
        for i in 0..<8 {
            let idx = isYangDun ? (sIdx + i) % 8 : (sIdx - i + 8) % 8
            map[ring[idx]] = EightDeity.allCases[i]
        }
        return map
    }

    private static func resolvedTimeZone(_ request: ChartRequest) -> TimeZone {
        if let offset = request.timeZoneSecondsFromGMT {
            return TimeZone(secondsFromGMT: offset) ?? .current
        }
        return .current
    }
}
