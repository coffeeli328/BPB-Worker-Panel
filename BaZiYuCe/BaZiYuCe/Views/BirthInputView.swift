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
        @Bindable var session = session
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

                            Text("分别填写出生年、月、日与时辰，排出四柱，并据流年、流月、流日给出规则化运势提示。")
                                .font(AppTheme.bodyFont)
                                .foregroundStyle(AppTheme.slate)
                                .fixedSize(horizontal: false, vertical: true)
                                .opacity(appeared ? 1 : 0)
                                .offset(y: appeared ? 0 : 8)
                        }
                        .padding(.top, 12)

                        VStack(alignment: .leading, spacing: 18) {
                            labeled("出生年月日") {
                                HStack(spacing: 10) {
                                    fieldColumn(title: "年") {
                                        Picker("年", selection: $session.birthYear) {
                                            ForEach(BaZiSession.minYear...BaZiSession.maxYear, id: \.self) { y in
                                                Text("\(y)年").tag(y)
                                            }
                                        }
                                        .pickerStyle(.wheel)
                                        .frame(maxWidth: .infinity)
                                        .clipped()
                                    }

                                    fieldColumn(title: "月") {
                                        Picker("月", selection: $session.birthMonth) {
                                            ForEach(1...12, id: \.self) { m in
                                                Text("\(m)月").tag(m)
                                            }
                                        }
                                        .pickerStyle(.wheel)
                                        .frame(maxWidth: .infinity)
                                        .clipped()
                                    }

                                    fieldColumn(title: "日") {
                                        Picker("日", selection: $session.birthDay) {
                                            ForEach(1...session.maxDayInMonth, id: \.self) { d in
                                                Text("\(d)日").tag(d)
                                            }
                                        }
                                        .pickerStyle(.wheel)
                                        .frame(maxWidth: .infinity)
                                        .clipped()
                                    }
                                }
                                .frame(height: 140)
                                .onChange(of: session.birthYear) { _, _ in session.clampBirthDay() }
                                .onChange(of: session.birthMonth) { _, _ in session.clampBirthDay() }
                            }

                            labeled("时辰") {
                                Picker("时辰", selection: $session.birthShiChenIndex) {
                                    ForEach(ShiChen.all) { sc in
                                        Text(sc.displayName).tag(sc.branch.rawValue)
                                    }
                                }
                                .pickerStyle(.wheel)
                                .frame(height: 120)
                                .clipped()

                                Text("当前：\(session.birthShiChen.displayName)")
                                    .font(.footnote)
                                    .foregroundStyle(AppTheme.slate)
                            }

                            labeled("性别") {
                                Picker("性别", selection: $session.gender) {
                                    ForEach(Gender.allCases) { g in
                                        Text(g.displayName).tag(g)
                                    }
                                }
                                .pickerStyle(.segmented)
                            }

                            if let err = session.inputError {
                                Text(err)
                                    .font(.footnote)
                                    .foregroundStyle(Color(red: 0.65, green: 0.22, blue: 0.18))
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
                                            Text(birthSummary)
                                                .font(.caption)
                                                .foregroundStyle(AppTheme.slate)
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

    private var birthSummary: String {
        "\(session.birthYear)年\(session.birthMonth)月\(session.birthDay)日 \(session.birthShiChen.branch.name)时 · \(session.gender.displayName)"
    }

    private func calculate() {
        withAnimation(.spring(response: 0.45, dampingFraction: 0.86)) {
            session.calculate()
            if session.inputError == nil {
                didCalculate = true
            }
        }
    }

    @ViewBuilder
    private func labeled<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(AppTheme.sectionFont)
                .foregroundStyle(AppTheme.celadonSoft)
                .tracking(1.2)
            content()
        }
    }

    @ViewBuilder
    private func fieldColumn<Content: View>(title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(spacing: 4) {
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundStyle(AppTheme.celadon)
            content()
        }
        .frame(maxWidth: .infinity)
    }
}
