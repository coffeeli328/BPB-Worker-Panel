//
//  ForecastCardView.swift
//  BaZiYuCe
//

import SwiftUI

struct ForecastCardView: View {
    let forecast: FortuneForecast
    var showDate: Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(forecast.period.sectionTitle)
                        .font(AppTheme.sectionFont)
                        .foregroundStyle(AppTheme.celadonSoft)
                    if showDate {
                        Text(dateLabel(forecast.targetDate, period: forecast.period))
                            .font(.caption)
                            .foregroundStyle(AppTheme.slate)
                    }
                    Text(forecast.flowingPillar.name)
                        .font(AppTheme.titleFont)
                        .foregroundStyle(AppTheme.ink)
                    Text("\(forecast.stemGod.name) · \(forecast.tone)")
                        .font(AppTheme.headlineFont)
                        .foregroundStyle(AppTheme.celadon)
                }
                Spacer()
                scoreRing(forecast.score)
            }

            Text(forecast.summary)
                .font(AppTheme.bodyFont)
                .foregroundStyle(AppTheme.ink)
                .fixedSize(horizontal: false, vertical: true)

            VStack(alignment: .leading, spacing: 10) {
                Text("运势提示")
                    .font(AppTheme.sectionFont)
                    .foregroundStyle(AppTheme.celadonSoft)
                ForEach(Array(forecast.tips.enumerated()), id: \.offset) { _, tip in
                    HStack(alignment: .top, spacing: 8) {
                        Circle()
                            .fill(AppTheme.accent)
                            .frame(width: 6, height: 6)
                            .padding(.top, 7)
                        Text(tip)
                            .font(AppTheme.bodyFont)
                            .foregroundStyle(AppTheme.ink)
                    }
                }
            }

            DisclosureGroup {
                VStack(alignment: .leading, spacing: 6) {
                    ForEach(forecast.ruleNotes, id: \.self) { note in
                        Text("· " + note)
                            .font(.footnote)
                            .foregroundStyle(AppTheme.slate)
                    }
                }
                .padding(.top, 6)
            } label: {
                Text("规则说明（可扩展）")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(AppTheme.celadon)
            }
        }
        .padding(18)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(.white.opacity(0.55))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(AppTheme.line, lineWidth: 1)
                )
        )
    }

    private func scoreRing(_ score: Int) -> some View {
        ZStack {
            Circle()
                .stroke(AppTheme.mistDeep, lineWidth: 8)
            Circle()
                .trim(from: 0, to: CGFloat(score) / 100)
                .stroke(AppTheme.celadon, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                .rotationEffect(.degrees(-90))
            VStack(spacing: 0) {
                Text("\(score)")
                    .font(.title2.weight(.semibold).monospacedDigit())
                    .foregroundStyle(AppTheme.ink)
                Text("强弱")
                    .font(.caption2)
                    .foregroundStyle(AppTheme.slate)
            }
        }
        .frame(width: 72, height: 72)
    }

    private func dateLabel(_ date: Date, period: FortunePeriod) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "zh_CN")
        switch period {
        case .year: f.dateFormat = "yyyy年"
        case .month: f.dateFormat = "yyyy年M月"
        case .day: f.dateFormat = "yyyy年M月d日"
        }
        return f.string(from: date)
    }
}

struct TimelineRowView: View {
    let forecast: FortuneForecast

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(dateLabel)
                    .font(.caption)
                    .foregroundStyle(AppTheme.slate)
                Text("\(forecast.flowingPillar.name) · \(forecast.stemGod.name)")
                    .font(.headline)
                    .foregroundStyle(AppTheme.ink)
                Text(forecast.tone)
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.celadon)
            }
            Spacer()
            Text("\(forecast.score)")
                .font(.title3.weight(.semibold).monospacedDigit())
                .foregroundStyle(strengthColor(forecast.score))
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(.white.opacity(0.5))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(AppTheme.line, lineWidth: 1)
                )
        )
    }

    private var dateLabel: String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "zh_CN")
        switch forecast.period {
        case .year: f.dateFormat = "yyyy年"
        case .month: f.dateFormat = "yyyy年M月"
        case .day: f.dateFormat = "M月d日 EEE"
        }
        return f.string(from: forecast.targetDate)
    }

    private func strengthColor(_ score: Int) -> Color {
        switch score {
        case 75...: return AppTheme.celadon
        case 55..<75: return AppTheme.accent
        default: return Color(red: 0.55, green: 0.28, blue: 0.26)
        }
    }
}
