#!/usr/bin/env python3
"""Golden checks mirroring BaZiYuCeAndroid engine (run without Android SDK)."""

from __future__ import annotations

STEMS = list("甲乙丙丁戊己庚辛壬癸")
BRANCHES = list("子丑寅卯辰巳午未申酉戌亥")


def sb(i: int) -> str:
    i %= 60
    return STEMS[i % 10] + BRANCHES[i % 12]


def julian_day(y: int, m: int, d: int) -> float:
    import math

    if m <= 2:
        y -= 1
        m += 12
    A = y // 100
    B = 2 - A + A // 4
    return math.floor(365.25 * (y + 4716)) + math.floor(30.6001 * (m + 1)) + d + B - 1524.5


def day_pillar(y: int, m: int, d: int, hour: int = 12) -> str:
    if hour >= 23:
        from datetime import date, timedelta

        dt = date(y, m, d) + timedelta(days=1)
        y, m, d = dt.year, dt.month, dt.day
    jd = julian_day(y, m, d)
    base_jd = 2415020.5  # 1900-01-01 0h UT
    base_index = 10  # 甲戌
    delta = int(jd - base_jd)
    idx = (base_index + delta) % 60
    return sb(idx)


def hour_pillar(day_stem: str, hour: int) -> str:
    branch_idx = ((hour + 1) // 2) % 12
    start = {"甲": 0, "己": 0, "乙": 2, "庚": 2, "丙": 4, "辛": 4, "丁": 6, "壬": 6, "戊": 8, "癸": 8}[day_stem]
    return STEMS[(start + branch_idx) % 10] + BRANCHES[branch_idx]


WX = {
    "甲": "木", "乙": "木", "丙": "火", "丁": "火", "戊": "土",
    "己": "土", "庚": "金", "辛": "金", "壬": "水", "癸": "水",
}
YY = {s: ("阳" if i % 2 == 0 else "阴") for i, s in enumerate(STEMS)}
ORDER = ["木", "火", "土", "金", "水"]


def ten_god(stem: str, day_master: str) -> str:
    me, other = WX[day_master], WX[stem]
    same = YY[stem] == YY[day_master]
    mi = ORDER.index(me)
    oi = ORDER.index(other)
    if other == me:
        return "比肩" if same else "劫财"
    if oi == (mi + 1) % 5:
        return "食神" if same else "伤官"
    if oi == (mi + 2) % 5:
        return "偏财" if same else "正财"
    if oi == (mi + 3) % 5:
        return "七杀" if same else "正官"
    return "偏印" if same else "正印"


def main() -> None:
    assert day_pillar(1900, 1, 1) == "甲戌", day_pillar(1900, 1, 1)
    assert day_pillar(2000, 1, 1) == "戊午", day_pillar(2000, 1, 1)
    assert day_pillar(2000, 1, 1, 23) == "己未"
    assert hour_pillar("甲", 0) == "甲子"
    assert hour_pillar("甲", 10) == "己巳"
    assert ten_god("乙", "甲") == "劫财"
    assert ten_god("丙", "甲") == "食神"
    assert ten_god("庚", "甲") == "七杀"
    print("OK: day/hour pillars + ten gods golden cases passed")


if __name__ == "__main__":
    main()
