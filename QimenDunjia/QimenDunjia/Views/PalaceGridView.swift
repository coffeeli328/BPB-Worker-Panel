//
//  PalaceGridView.swift
//  QimenDunjia
//

import SwiftUI

struct PalaceGridView: View {
    let chart: QimenChart

    var body: some View {
        VStack(spacing: 4) {
            Text("南")
                .font(.caption2)
                .foregroundStyle(AppTheme.muted)
            ForEach(0..<3, id: \.self) { row in
                HStack(spacing: 4) {
                    if row == 1 {
                        Text("东").font(.caption2).foregroundStyle(AppTheme.muted).frame(width: 14)
                    } else {
                        Color.clear.frame(width: 14)
                    }
                    ForEach(0..<3, id: \.self) { col in
                        let palace = Palace.gridOrder[row][col]
                        PalaceCellView(cell: chart.cell(for: palace), isYangDun: chart.isYangDun)
                    }
                    if row == 1 {
                        Text("西").font(.caption2).foregroundStyle(AppTheme.muted).frame(width: 14)
                    } else {
                        Color.clear.frame(width: 14)
                    }
                }
            }
            Text("北")
                .font(.caption2)
                .foregroundStyle(AppTheme.muted)
        }
    }
}

struct PalaceCellView: View {
    let cell: PalaceCell?
    let isYangDun: Bool

    var body: some View {
        let p = cell?.palace
        VStack(alignment: .leading, spacing: 2) {
            HStack {
                Text(p.map { "\($0.name)\($0.rawValue)" } ?? "")
                    .font(.caption.weight(.semibold))
                Spacer()
                if cell?.isZhiFu == true {
                    Text("符").font(.caption2).foregroundStyle(AppTheme.cinnabar)
                }
                if cell?.isZhiShi == true {
                    Text("使").font(.caption2).foregroundStyle(AppTheme.pine)
                }
                if cell?.isEmpty == true {
                    Text("空").font(.caption2).foregroundStyle(AppTheme.muted)
                }
            }
            if p != .zhong5 {
                Text(cell?.deity?.name(isYangDun: isYangDun) ?? "—")
                    .font(.caption2)
                    .foregroundStyle(AppTheme.muted)
            }
            Text(cell?.star?.shortName ?? "—")
                .font(.subheadline.weight(.medium))
            HStack {
                Text(cell?.heavenStem?.name ?? "·")
                Text("/")
                Text(cell?.earthStem?.name ?? "·")
                    .foregroundStyle(AppTheme.muted)
            }
            .font(.caption.monospaced())
            if p != .zhong5 {
                Text(cell?.gate?.displayName ?? "—")
                    .font(.caption)
            }
        }
        .padding(8)
        .frame(maxWidth: .infinity, minHeight: 96, alignment: .topLeading)
        .background(AppTheme.wash)
        .overlay(
            RoundedRectangle(cornerRadius: 4)
                .stroke(AppTheme.ink.opacity(0.15), lineWidth: 1)
        )
    }
}
