//
//  InterpretationView.swift
//  QimenDunjia
//

import SwiftUI

struct InterpretationView: View {
    let chart: QimenChart

    var body: some View {
        List(chart.interpretations) { item in
            VStack(alignment: .leading, spacing: 4) {
                Text(item.title)
                    .font(.headline)
                    .foregroundStyle(color(for: item.tone))
                Text(item.detail)
                    .font(.body)
                    .foregroundStyle(AppTheme.ink)
            }
            .padding(.vertical, 4)
            .listRowBackground(AppTheme.paper)
        }
        .scrollContentBackground(.hidden)
        .background(AppTheme.paper)
        .navigationTitle("解读")
    }

    private func color(for tone: InterpretationItem.Tone) -> Color {
        switch tone {
        case .auspicious: return AppTheme.pine
        case .caution: return AppTheme.cinnabar
        case .neutral: return AppTheme.ink
        }
    }
}
