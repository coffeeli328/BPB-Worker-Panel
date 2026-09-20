//
//  InterpretationView.swift
//  QimenDunjia
//

import SwiftUI

struct InterpretationView: View {
    let chart: QimenChart

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                if chart.hasQuestion {
                    Text("所问：\(chart.question)")
                        .font(.system(size: 17, weight: .semibold, design: .serif))
                        .foregroundStyle(AppTheme.ink)
                        .padding(.bottom, 4)
                    Text("事项：\(chart.questionTopic.rawValue) · 规则模板，仅供参考")
                        .font(.caption)
                        .foregroundStyle(AppTheme.muted)
                        .padding(.bottom, 16)
                } else {
                    Text("尚未填写所问之事。请返回起局页填写后再排盘，以获得针对性解读。")
                        .font(.subheadline)
                        .foregroundStyle(AppTheme.cinnabar)
                        .padding(.bottom, 16)
                }

                ForEach(chart.interpretations) { item in
                    HStack(alignment: .top, spacing: 12) {
                        RoundedRectangle(cornerRadius: 1)
                            .fill(toneColor(item.tone))
                            .frame(width: 3)
                            .padding(.vertical, 2)

                        VStack(alignment: .leading, spacing: 4) {
                            HStack(spacing: 8) {
                                Text(item.title)
                                    .font(.system(size: 16, weight: .semibold, design: .serif))
                                    .foregroundStyle(AppTheme.ink)
                                Text(toneLabel(item.tone))
                                    .font(.system(size: 10, weight: .medium))
                                    .foregroundStyle(toneColor(item.tone))
                            }
                            Text(item.detail)
                                .font(.system(size: 15))
                                .foregroundStyle(AppTheme.muted)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                    .padding(.vertical, 12)
                    .overlay(alignment: .bottom) {
                        Rectangle()
                            .fill(AppTheme.line.opacity(0.7))
                            .frame(height: 1)
                    }
                }
            }
            .padding(20)
        }
        .background(AppTheme.screenBackground)
        .navigationTitle(chart.hasQuestion ? "问事解读" : "简要解读")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func toneColor(_ tone: InterpretationItem.Tone) -> Color {
        switch tone {
        case .auspicious: return AppTheme.pine
        case .caution: return AppTheme.cinnabar
        case .neutral: return AppTheme.ink.opacity(0.45)
        }
    }

    private func toneLabel(_ tone: InterpretationItem.Tone) -> String {
        switch tone {
        case .auspicious: return "宜"
        case .caution: return "慎"
        case .neutral: return "述"
        }
    }
}
