//
//  PalaceGridView.swift
//  QimenDunjia
//
//  九宫盘：单一构图。层次 — 八神 / 九星 / 天盘干·地盘干 / 八门；符·使·空轻标。
//

import SwiftUI

struct PalaceGridView: View {
    let chart: QimenChart

    var body: some View {
        VStack(spacing: 10) {
            legendRow

            VStack(spacing: 0) {
                Text("南")
                    .font(.system(size: 10, weight: .medium, design: .serif))
                    .foregroundStyle(AppTheme.muted)
                    .padding(.bottom, 6)

                HStack(alignment: .center, spacing: 6) {
                    Text("东")
                        .font(.system(size: 10, weight: .medium, design: .serif))
                        .foregroundStyle(AppTheme.muted)
                        .frame(width: 14)

                    // One framed composition — not nine floating cards
                    VStack(spacing: 0) {
                        ForEach(0..<3, id: \.self) { row in
                            HStack(spacing: 0) {
                                ForEach(0..<3, id: \.self) { col in
                                    let palace = Palace.gridOrder[row][col]
                                    PalaceCellView(
                                        cell: chart.cell(for: palace),
                                        isYangDun: chart.isYangDun
                                    )
                                    if col < 2 {
                                        Rectangle()
                                            .fill(AppTheme.line)
                                            .frame(width: 1)
                                    }
                                }
                            }
                            if row < 2 {
                                Rectangle()
                                    .fill(AppTheme.line)
                                    .frame(height: 1)
                            }
                        }
                    }
                    .background(AppTheme.wash.opacity(0.55))
                    .overlay(
                        Rectangle()
                            .stroke(AppTheme.ink.opacity(0.35), lineWidth: 1.5)
                    )

                    Text("西")
                        .font(.system(size: 10, weight: .medium, design: .serif))
                        .foregroundStyle(AppTheme.muted)
                        .frame(width: 14)
                }

                Text("北")
                    .font(.system(size: 10, weight: .medium, design: .serif))
                    .foregroundStyle(AppTheme.muted)
                    .padding(.top, 6)
            }
        }
    }

    private var legendRow: some View {
        HStack(spacing: 14) {
            legendItem(color: AppTheme.heavenStem, label: "天盘干")
            legendItem(color: AppTheme.earthStem, label: "地盘干")
            HStack(spacing: 2) {
                Text("符")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(AppTheme.cinnabar)
                Text("值符")
                    .foregroundStyle(AppTheme.muted)
            }
            HStack(spacing: 2) {
                Text("使")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(AppTheme.pine)
                Text("值使")
                    .foregroundStyle(AppTheme.muted)
            }
            Spacer(minLength: 0)
        }
        .font(.caption2)
    }

    private func legendItem(color: Color, label: String) -> some View {
        HStack(spacing: 4) {
            Circle()
                .fill(color)
                .frame(width: 5, height: 5)
            Text(label)
                .foregroundStyle(AppTheme.muted)
        }
    }
}

struct PalaceCellView: View {
    let cell: PalaceCell?
    let isYangDun: Bool

    private var isZhong: Bool { cell?.palace == .zhong5 }

    var body: some View {
        let p = cell?.palace
        VStack(alignment: .leading, spacing: 3) {
            // Header: palace + markers
            HStack(alignment: .firstTextBaseline, spacing: 3) {
                Text(p.map { "\($0.name)\($0.rawValue)" } ?? "")
                    .font(.system(size: 11, weight: .semibold, design: .serif))
                    .foregroundStyle(AppTheme.ink)
                Spacer(minLength: 0)
                if cell?.isEmpty == true {
                    Text("空")
                        .font(.system(size: 9, weight: .medium))
                        .foregroundStyle(AppTheme.muted)
                }
                if cell?.isZhiFu == true {
                    Text("符")
                        .font(.system(size: 9, weight: .bold))
                        .foregroundStyle(AppTheme.cinnabar)
                }
                if cell?.isZhiShi == true {
                    Text("使")
                        .font(.system(size: 9, weight: .bold))
                        .foregroundStyle(AppTheme.pine)
                }
            }

            if !isZhong {
                Text(cell?.deity.map { $0.name(isYangDun: isYangDun) } ?? "·")
                    .font(.system(size: 10))
                    .foregroundStyle(AppTheme.muted)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            } else {
                Text("中五")
                    .font(.system(size: 10))
                    .foregroundStyle(AppTheme.muted)
            }

            // Star — primary signal
            Text(cell?.star?.name ?? "·")
                .font(.system(size: 15, weight: .semibold, design: .serif))
                .foregroundStyle(AppTheme.ink)
                .lineLimit(1)
                .minimumScaleFactor(0.7)

            // Heaven / Earth stems
            HStack(alignment: .firstTextBaseline, spacing: 2) {
                Text(cell?.heavenStem?.name ?? "·")
                    .font(.system(size: 16, weight: .bold, design: .serif))
                    .foregroundStyle(AppTheme.heavenStem)
                Text("·")
                    .font(.system(size: 11))
                    .foregroundStyle(AppTheme.line)
                Text(cell?.earthStem?.name ?? "·")
                    .font(.system(size: 13, weight: .regular, design: .serif))
                    .foregroundStyle(AppTheme.earthStem)
            }

            if !isZhong {
                Text(cell?.gate?.displayName ?? "·")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(AppTheme.pine)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            } else {
                Text(" ")
                    .font(.system(size: 12))
            }
        }
        .padding(.horizontal, 7)
        .padding(.vertical, 8)
        .frame(maxWidth: .infinity, minHeight: 108, alignment: .topLeading)
        .background(cellBackground)
    }

    private var cellBackground: Color {
        if cell?.isZhiFu == true || cell?.isZhiShi == true {
            return AppTheme.washDeep.opacity(0.45)
        }
        if isZhong {
            return AppTheme.wash.opacity(0.35)
        }
        return Color.clear
    }
}
