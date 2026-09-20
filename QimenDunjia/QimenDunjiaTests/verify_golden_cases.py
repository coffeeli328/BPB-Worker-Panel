#!/usr/bin/env python3
"""Mirror of QimenEngine plate logic for golden-case verification on Linux."""

from __future__ import annotations

STEMS = list("甲乙丙丁戊己庚辛壬癸")
BRANCHES = list("子丑寅卯辰巳午未申酉戌亥")
PALACE_NAMES = {1: "坎", 2: "坤", 3: "震", 4: "巽", 5: "中", 6: "乾", 7: "兑", 8: "艮", 9: "离"}
YANG_FLY = [1, 2, 3, 4, 5, 6, 7, 8, 9]
YIN_FLY = [9, 8, 7, 6, 5, 4, 3, 2, 1]
CLOCK = [1, 8, 3, 4, 9, 2, 7, 6]  # 坎艮震巽离坤兑乾
STAR_CLOCK = ["蓬", "任", "冲", "辅", "英", "芮", "柱", "心"]
GATE_CLOCK = ["休", "生", "伤", "杜", "景", "死", "惊", "开"]
GATE_INNATE = {1: "休", 2: "死", 3: "伤", 4: "杜", 9: "景", 6: "开", 7: "惊", 8: "生"}
QI_YI = list("戊己庚辛壬癸丁丙乙")
XUN_YI = {0: "戊", 10: "己", 20: "庚", 30: "辛", 40: "壬", 50: "癸"}
STAR_OF = {1: "蓬", 2: "芮", 3: "冲", 4: "辅", 5: "禽", 6: "心", 7: "柱", 8: "任", 9: "英"}
ZHONG_HOST = 2


def sb_index(stem: str, branch: str) -> int:
    for i in range(60):
        if STEMS[i % 10] == stem and BRANCHES[i % 12] == branch:
            return i
    raise ValueError(stem + branch)


def earth_plate(is_yang: bool, ju: int) -> dict[int, str]:
    order = YANG_FLY if is_yang else YIN_FLY
    s = order.index(ju)
    return {order[(s + i) % 9]: QI_YI[i] for i in range(9)}


def build_plate(is_yang: bool, ju: int, hour_stem: str, hour_branch: str):
    earth = earth_plate(is_yang, ju)
    hidx = sb_index(hour_stem, hour_branch)
    xun = (hidx // 10) * 10
    yi = XUN_YI[xun]
    xun_palace = next(p for p, s in earth.items() if s == yi)
    zhi_fu_star = STAR_OF[xun_palace]
    zhi_shi_gate = "死" if xun_palace == 5 else GATE_INNATE[xun_palace]

    use_stem = yi if hour_stem == "甲" else hour_stem
    hour_palace = next(p for p, s in earth.items() if s == use_stem)
    if hour_palace == 5:
        hour_palace = ZHONG_HOST

    # stars
    pivot = "芮" if zhi_fu_star == "禽" else zhi_fu_star
    from_i = STAR_CLOCK.index(pivot)
    to_i = CLOCK.index(hour_palace)
    shift = (to_i - from_i) % 8
    stars = {CLOCK[i]: STAR_CLOCK[(i - shift) % 8] for i in range(8)}
    stars[5] = "禽"

    # zhi shi move
    fly = YANG_FLY if is_yang else YIN_FLY
    offset = hidx - xun
    sidx = fly.index(xun_palace)
    zhi_shi_raw = fly[(sidx + (offset % 9)) % 9]
    zhi_shi = ZHONG_HOST if zhi_shi_raw == 5 else zhi_shi_raw
    g_from = GATE_CLOCK.index(zhi_shi_gate)
    g_to = CLOCK.index(zhi_shi)
    g_shift = (g_to - g_from) % 8
    gates = {CLOCK[i]: GATE_CLOCK[(i - g_shift) % 8] for i in range(8)}

    # deities
    d_start = CLOCK.index(hour_palace)
    deities = {}
    names = ["符", "蛇", "阴", "合", "虎", "武", "地", "天"]
    for i, name in enumerate(names):
        idx = (d_start + i) % 8 if is_yang else (d_start - i) % 8
        deities[CLOCK[idx]] = name

    xun_kong = {
        0: ["戌", "亥"],
        10: ["申", "酉"],
        20: ["午", "未"],
        30: ["辰", "巳"],
        40: ["寅", "卯"],
        50: ["子", "丑"],
    }[xun]

    return {
        "earth": earth,
        "stars": stars,
        "gates": gates,
        "deities": deities,
        "zhi_fu_star": zhi_fu_star,
        "zhi_shi_gate": zhi_shi_gate,
        "zhi_fu_palace": hour_palace,
        "zhi_shi_palace": zhi_shi,
        "xun_kong": xun_kong,
        "yi": yi,
        "xun_palace": xun_palace,
    }


def assert_eq(label, got, exp):
    if got != exp:
        raise AssertionError(f"{label}: got {got!r} expected {exp!r}")


def test_yang1_jiazi():
    """阳遁一局甲子时 — 标准伏吟盘。"""
    p = build_plate(True, 1, "甲", "子")
    assert_eq("earth", p["earth"], {1: "戊", 2: "己", 3: "庚", 4: "辛", 5: "壬", 6: "癸", 7: "丁", 8: "丙", 9: "乙"})
    assert_eq("zhi_fu", p["zhi_fu_star"], "蓬")
    assert_eq("zhi_shi", p["zhi_shi_gate"], "休")
    assert_eq("zhi_fu_palace", p["zhi_fu_palace"], 1)
    assert_eq("zhi_shi_palace", p["zhi_shi_palace"], 1)
    assert_eq("xun_kong", p["xun_kong"], ["戌", "亥"])
    # 星伏吟
    for pal, star in [(1, "蓬"), (8, "任"), (3, "冲"), (4, "辅"), (9, "英"), (2, "芮"), (7, "柱"), (6, "心")]:
        assert_eq(f"star@{pal}", p["stars"][pal], star)
    # 门伏吟
    for pal, gate in [(1, "休"), (8, "生"), (3, "伤"), (4, "杜"), (9, "景"), (2, "死"), (7, "惊"), (6, "开")]:
        assert_eq(f"gate@{pal}", p["gates"][pal], gate)
    # 八神自坎顺
    assert_eq("deities", p["deities"], {1: "符", 8: "蛇", 3: "阴", 4: "合", 9: "虎", 2: "武", 7: "地", 6: "天"})


def test_yang1_dingmao():
    """阳遁一局丁卯时 — 教材常见例。"""
    p = build_plate(True, 1, "丁", "卯")
    assert_eq("zhi_fu", (p["zhi_fu_star"], p["zhi_fu_palace"]), ("蓬", 7))
    assert_eq("zhi_shi", (p["zhi_shi_gate"], p["zhi_shi_palace"]), ("休", 4))
    assert_eq("star@7", p["stars"][7], "蓬")
    assert_eq("gate@4", p["gates"][4], "休")


def test_yang6_yisi():
    """阳遁六局乙巳时 — zyqmdj 拆补法举例。"""
    p = build_plate(True, 6, "乙", "巳")
    # 戊起六宫：6戊 7己 8庚 9辛 1壬 2癸 3丁 4丙 5乙
    assert_eq("earth", p["earth"], {6: "戊", 7: "己", 8: "庚", 9: "辛", 1: "壬", 2: "癸", 3: "丁", 4: "丙", 5: "乙"})
    assert_eq("yi", p["yi"], "壬")
    assert_eq("xun_palace", p["xun_palace"], 1)
    assert_eq("zhi_fu", p["zhi_fu_star"], "蓬")
    assert_eq("zhi_shi", p["zhi_shi_gate"], "休")
    # 乙在中五寄坤二
    assert_eq("zhi_fu_palace", p["zhi_fu_palace"], 2)
    assert_eq("zhi_shi_palace", p["zhi_shi_palace"], 2)
    assert_eq("deities", p["deities"], {2: "符", 7: "蛇", 6: "阴", 1: "合", 8: "虎", 3: "武", 4: "地", 9: "天"})


def test_yin9_bingyin():
    """阴遁九局丙寅时 — 值使逆飞。"""
    p = build_plate(False, 9, "丙", "寅")
    # 戊起九逆：9戊 8己 7庚 6辛 5壬 4癸 3丁 2丙 1乙
    assert_eq("earth@9", p["earth"][9], "戊")
    assert_eq("earth@2", p["earth"][2], "丙")
    assert_eq("zhi_fu_star", p["zhi_fu_star"], "英")  # 戊在九→天英
    assert_eq("zhi_shi_gate", p["zhi_shi_gate"], "景")
    # 丙在坤二 → 值符英到二
    assert_eq("zhi_fu_palace", p["zhi_fu_palace"], 2)
    # 甲子旬丙寅 offset=2，自九逆：9,8,7 → 兑七
    assert_eq("zhi_shi_palace", p["zhi_shi_palace"], 7)


def test_ju_table():
    table = {
        ("冬至", 0): (True, 1),
        ("芒种", 0): (True, 6),
        ("夏至", 0): (False, 9),
        ("立夏", 2): (True, 7),  # 立夏下元阳7
    }
    term_ju = {
        "冬至": (True, [1, 7, 4]),
        "芒种": (True, [6, 3, 9]),
        "夏至": (False, [9, 3, 6]),
        "立夏": (True, [4, 1, 7]),
    }
    for (term, yi), (yang, ju) in table.items():
        e = term_ju[term]
        assert_eq(f"ju {term}/{yi}", (e[0], e[1][yi]), (yang, ju))


def test_futou_yuan():
    def futou(stem, branch):
        idx = sb_index(stem, branch)
        for _ in range(10):
            s, b = STEMS[idx % 10], BRANCHES[idx % 12]
            if s in ("甲", "己"):
                return s + b
            idx = (idx + 59) % 60
        return None

    def yuan(ft):
        b = ft[1]
        if b in "子午卯酉":
            return 0
        if b in "寅申巳亥":
            return 1
        return 2

    assert_eq("futou 壬午", futou("壬", "午"), "己卯")
    assert_eq("yuan 己卯", yuan("己卯"), 0)
    # 壬戌向前取最近甲/己 → 己未（未属下元）；非甲戌
    assert_eq("futou 壬戌", futou("壬", "戌"), "己未")
    assert_eq("yuan 己未", yuan("己未"), 2)
    assert_eq("yuan 甲戌", yuan("甲戌"), 2)


if __name__ == "__main__":
    test_ju_table()
    test_futou_yuan()
    test_yang1_jiazi()
    test_yang1_dingmao()
    test_yang6_yisi()
    test_yin9_bingyin()
    print("ALL GOLDEN CASES PASSED")
