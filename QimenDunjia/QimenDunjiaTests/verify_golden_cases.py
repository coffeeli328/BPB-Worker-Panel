#!/usr/bin/env python3
"""Golden + astronomy regression tests (Linux mirror of Swift engine)."""

from __future__ import annotations

import math
from datetime import datetime, timezone, timedelta

# --- plate engine (unchanged core) ---
STEMS = list("甲乙丙丁戊己庚辛壬癸")
BRANCHES = list("子丑寅卯辰巳午未申酉戌亥")
YANG_FLY = [1, 2, 3, 4, 5, 6, 7, 8, 9]
YIN_FLY = [9, 8, 7, 6, 5, 4, 3, 2, 1]
CLOCK = [1, 8, 3, 4, 9, 2, 7, 6]
STAR_CLOCK = ["蓬", "任", "冲", "辅", "英", "芮", "柱", "心"]
GATE_CLOCK = ["休", "生", "伤", "杜", "景", "死", "惊", "开"]
GATE_INNATE = {1: "休", 2: "死", 3: "伤", 4: "杜", 9: "景", 6: "开", 7: "惊", 8: "生"}
QI_YI = list("戊己庚辛壬癸丁丙乙")
XUN_YI = {0: "戊", 10: "己", 20: "庚", 30: "辛", 40: "壬", 50: "癸"}
STAR_OF = {1: "蓬", 2: "芮", 3: "冲", 4: "辅", 5: "禽", 6: "心", 7: "柱", 8: "任", 9: "英"}
ZHONG_HOST = 2
J2000 = 2451545.0
D2R = math.pi / 180
NAMES = [
    "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
    "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
    "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
    "寒露", "霜降", "立冬", "小雪", "大雪", "冬至",
]
LONS = [285, 300, 315, 330, 345, 0, 15, 30, 45, 60, 75, 90,
        105, 120, 135, 150, 165, 180, 195, 210, 225, 240, 255, 270]


def assert_eq(label, got, exp):
    if got != exp:
        raise AssertionError(f"{label}: got {got!r} expected {exp!r}")


def assert_true(label, cond):
    if not cond:
        raise AssertionError(label)


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
    pivot = "芮" if zhi_fu_star == "禽" else zhi_fu_star
    from_i = STAR_CLOCK.index(pivot)
    to_i = CLOCK.index(hour_palace)
    shift = (to_i - from_i) % 8
    stars = {CLOCK[i]: STAR_CLOCK[(i - shift) % 8] for i in range(8)}
    stars[5] = "禽"
    fly = YANG_FLY if is_yang else YIN_FLY
    offset = hidx - xun
    sidx = fly.index(xun_palace)
    zhi_shi_raw = fly[(sidx + (offset % 9)) % 9]
    zhi_shi = ZHONG_HOST if zhi_shi_raw == 5 else zhi_shi_raw
    g_from = GATE_CLOCK.index(zhi_shi_gate)
    g_to = CLOCK.index(zhi_shi)
    g_shift = (g_to - g_from) % 8
    gates = {CLOCK[i]: GATE_CLOCK[(i - g_shift) % 8] for i in range(8)}
    return {
        "earth": earth,
        "stars": stars,
        "gates": gates,
        "zhi_fu_star": zhi_fu_star,
        "zhi_shi_gate": zhi_shi_gate,
        "zhi_fu_palace": hour_palace,
        "zhi_shi_palace": zhi_shi,
    }


# --- astronomy ---
def rev360(x):
    return x % 360


def delta_t(year: float) -> float:
    y = year
    t = y - 2000.0
    if y < 1900 or y > 2100:
        u = (y - 1820.0) / 100.0
        return -20.0 + 32.0 * u * u
    if y < 2005:
        u = y - 2000
        return 63.86 + 0.3345 * u - 0.060374 * u * u + 0.0017275 * u ** 3
    if y < 2050:
        return 62.92 + 0.32217 * t + 0.005589 * t * t
    return -20 + 32 * ((y - 1820) / 100) ** 2 - 0.5628 * (2100 - y)


def sun_lon(jde):
    T = (jde - J2000) / 36525.0
    L0 = rev360(280.46646 + 36000.76983 * T + 0.0003032 * T * T)
    M = rev360(357.52911 + 35999.05029 * T - 0.0001537 * T * T)
    Mr = M * D2R
    C = ((1.914602 - 0.004817 * T - 0.000014 * T * T) * math.sin(Mr)
         + (0.019993 - 0.000101 * T) * math.sin(2 * Mr)
         + 0.000289 * math.sin(3 * Mr))
    omega = 125.04 - 1934.136 * T
    return rev360(L0 + C - 0.00569 - 0.00478 * math.sin(omega * D2R))


def solve_lon(target, guess):
    jde = guess
    for _ in range(16):
        lon = sun_lon(jde)
        diff = lon - target
        while diff > 180:
            diff -= 360
        while diff < -180:
            diff += 360
        h = 0.02
        d1 = sun_lon(jde + h) - sun_lon(jde - h)
        while d1 > 180:
            d1 -= 360
        while d1 < -180:
            d1 += 360
        jde -= diff / (d1 / (2 * h))
    return jde


def jd_ut_ymd(y, m, d, hour=0.0):
    a = (14 - m) // 12
    yy = y + 4800 - a
    mm = m + 12 * a - 3
    jdn = d + (153 * mm + 2) // 5 + 365 * yy + yy // 4 - yy // 100 + yy // 400 - 32045
    return jdn + (hour - 12) / 24.0


def term_ut(year, idx):
    guess = jd_ut_ymd(year, 1, 1) + 5 + idx * 15.2184
    jde = solve_lon(LONS[idx], guess)
    return jde - delta_t(year) / 86400.0


def eot_minutes(jde):
    T = (jde - J2000) / 36525.0
    L0 = rev360(280.46646 + 36000.76983 * T + 0.0003032 * T * T)
    M = 357.52911 + 35999.05029 * T - 0.0001537 * T * T
    e = 0.016708634 - T * (0.000042037 + 0.0000001267 * T)
    eps0 = 23 + (26 + (21.448 - T * (46.815 + T * (0.00059 - T * 0.001813))) / 60) / 60
    omega = 125.04 - 1934.136 * T
    eps = eps0 + 0.00256 * math.cos(omega * D2R)
    y = math.tan(eps / 2 * D2R) ** 2
    Mr = M * D2R
    L0r = L0 * D2R
    eot_rad = (y * math.sin(2 * L0r) - 2 * e * math.sin(Mr)
               + 4 * e * y * math.sin(Mr) * math.cos(2 * L0r)
               - 0.5 * y * y * math.sin(4 * L0r) - 1.25 * e * e * math.sin(2 * Mr))
    return 4 * math.degrees(eot_rad)


def hour_branch_from_hour(hour: float) -> str:
    # 23-1 子, 1-3 丑 ...
    h = int(math.floor(hour)) % 24
    idx = ((h + 1) // 2) % 12
    return BRANCHES[idx]


def true_solar_shift_minutes(longitude, tz_meridian, eot):
    return (longitude - tz_meridian) * 4.0 + eot


# --- tests ---
def test_plate_core():
    p = build_plate(True, 1, "甲", "子")
    assert_eq("伏吟符", (p["zhi_fu_star"], p["zhi_fu_palace"]), ("蓬", 1))
    p2 = build_plate(True, 1, "丁", "卯")
    assert_eq("丁卯", (p2["zhi_fu_palace"], p2["zhi_shi_palace"]), (7, 4))


def test_solar_terms_known_window():
    """2024 立春应在 2/4 UTC 早晨（CST 下午）；交节前后术语切换。"""
    jd = term_ut(2024, NAMES.index("立春"))
    # ~ 2024-02-04 08:2x UTC
    assert_true("立春在2月4日附近", 2460344.8 < jd < 2460345.0)
    # 春分 ~ Mar 20
    jd2 = term_ut(2024, NAMES.index("春分"))
    assert_true("春分3月", 2460389.5 < jd2 < 2460390.0)
    # 冬至 ~ Dec 21
    jd3 = term_ut(2024, NAMES.index("冬至"))
    assert_true("冬至12月", 2460665.7 < jd3 < 2460666.1)


def test_term_boundary_flip():
    """交节前为前一节气，交节后为立春。"""
    jd_lichun = term_ut(2024, NAMES.index("立春"))
    before = jd_lichun - 5 / 1440  # 5 minutes before
    after = jd_lichun + 5 / 1440

    def current_name(jd):
        # scan 2023-2024 terms
        best = None
        for y in (2023, 2024):
            for i, name in enumerate(NAMES):
                t = term_ut(y, i)
                if t <= jd and (best is None or t > best[0]):
                    best = (t, name)
        return best[1]

    assert_eq("before 立春", current_name(before), "大寒")
    assert_eq("after 立春", current_name(after), "立春")


def test_eot_meeus_example():
    """1992-10-13 EoT ≈ +13.7 分钟（Meeus）。"""
    jd = jd_ut_ymd(1992, 10, 13, 0)
    eot = eot_minutes(jd + 0.5)  # midday-ish
    assert_true(f"EoT~13–14 got {eot}", 12.5 < eot < 15.0)


def test_longitude_shifts_hour_branch():
    """同一钟表时，乌鲁木齐 vs 东经120° 可跨时辰。"""
    # Civil 00:50 CST (UTC+8). Meridian 120°.
    # At 120°E, lon corr=0; EoT ignore for extreme lon test use fixed eot=0
    eot = 0.0
    # 乌鲁木齐 ~87.6 → (87.6-120)*4 = -129.6 min ≈ -2h10m
    shift_w = true_solar_shift_minutes(87.6168, 120.0, eot)
    shift_b = true_solar_shift_minutes(120.0, 120.0, eot)
    civil_hour = 0 + 50 / 60  # 00:50
    h_w = (civil_hour * 60 + shift_w) / 60
    h_b = (civil_hour * 60 + shift_b) / 60
    # normalize
    h_w %= 24
    h_b %= 24
    br_w = hour_branch_from_hour(h_w)
    br_b = hour_branch_from_hour(h_b)
    assert_eq("120° at 00:50", br_b, "子")
    # 00:50 - 2h10 ≈ 22:40 → 亥
    assert_eq("乌市真太阳", br_w, "亥")
    assert_true("经度导致时辰不同", br_w != br_b)


def test_beijing_vs_meridian_small_shift():
    """北京 116.4 vs 120：改正约 -14.4 分，通常不跨时辰，但量级正确。"""
    shift = true_solar_shift_minutes(116.4074, 120.0, 0)
    assert_true(f"北京改正~-14.4 got {shift}", -15.0 < shift < -13.5)


def test_timezone_meridian():
    """UTC+8 中央经线 120；UTC+9 → 135。"""
    assert_eq("utc8", 8 * 15, 120)
    assert_eq("utc9", 9 * 15, 135)


if __name__ == "__main__":
    test_plate_core()
    test_solar_terms_known_window()
    test_term_boundary_flip()
    test_eot_meeus_example()
    test_longitude_shifts_hour_branch()
    test_beijing_vs_meridian_small_shift()
    test_timezone_meridian()
    print("ALL GOLDEN + ASTRONOMY CASES PASSED")
