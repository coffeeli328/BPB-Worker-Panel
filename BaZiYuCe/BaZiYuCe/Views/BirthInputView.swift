//
//  BirthInputView.swift
//  BaZiYuCe
//

import SwiftUI

struct BirthInputView: View {
    @Environment(BaZiSession.self) private var session
    @State private var appeared = false
    @State private var didCalculate = false

    var body: some View {
        NavigationStack {
            ZStack {
                AppTheme.screenBackground

                ScrollView {
                    VStack(alignment: .leading, spacing: 28) {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("八字运势")
                                .font(AppTheme.brandFont)
                                .foregroundStyle(AppTheme.ink)
                                .opacity(appeared ? 1 : 0)
                                .offset(y: appeared ? 0 : 12)

                            Text("输入出生时间，排出四柱，并据流年、流月、流日给出规则化运势提示。")
                                .font(AppTheme.bodyFont)
                                .foregroundStyle(AppTheme.slate)
                                .fixedSize(horizontal: false, vertical: true)
                                .opacity(appeared ? 1 : 0)
                                .offset(y: appeared ? 0 : 8)
                        }
                        .padding(.top, 12)

                        VStack(alignment: .leading, spacing: 18) {
                            labeled("出生日期与时辰") {
                                DatePicker(
                                    "出生时间",
                                    selection: Bindable(session).birthDate,
                                    displayedComponents: [.date, .hourAndMinute]
                                )
                                .datePickerStyle(.graphical)
                                .tint(AppTheme.celadon)
                                .environment(\.locale, Locale(identifier: "zh_CN"))
                            }

                            labeled("性别") {
                                Picker("性别", selection: Bindable(session).gender) {
                                    ForEach(Gender.allCases) { g in
                                        Text(g.displayName).tag(g)
                                    }
                                }
                                .pickerStyle(.segmented)
                            }

                            Button(action: calculate) {
                                Text(didCalculate ? "重新排盘" : "排出八字")
                                    .font(.headline)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 14)
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(AppTheme.celadon)
                            .padding(.top, 4)

                            if let chart = session.chart {
                                NavigationLink {
                                    ChartResultView()
                                } label: {
                                    HStack {
                                        VStack(alignment: .leading, spacing: 4) {
                                            Text("四柱已成")
                                                .font(AppTheme.sectionFont)
                                                .foregroundStyle(AppTheme.celadon)
                                            Text(chart.fourPillarsText)
                                                .font(AppTheme.headlineFont)
                                                .foregroundStyle(AppTheme.ink)
                                        }
                                        Spacer()
                                        Image(systemName: "chevron.right")
                                            .foregroundStyle(AppTheme.slate)
                                    }
                                    .padding(16)
                                    .background(
                                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                                            .fill(.white.opacity(0.55))
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 14, style: .continuous)
                                                    .stroke(AppTheme.line, lineWidth: 1)
                                            )
                                    )
                                }
                                .transition(.move(edge: .bottom).combined(with: .opacity))
                            }
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 40)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                withAnimation(.easeOut(duration: 0.55)) { appeared = true }
            }
        }
    }

    private func calculate() {
        withAnimation(.spring(response: 0.45, dampingFraction: 0.86)) {
            session.calculate()
            didCalculate = true
        }
    }

    @ViewBuilder
    private func labeled<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title.uppercased())
                .font(AppTheme.sectionFont)
                .foregroundStyle(AppTheme.celadonSoft)
                .tracking(1.2)
            content()
        }
    }
}
