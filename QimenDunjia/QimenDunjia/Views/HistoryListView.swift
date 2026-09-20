//
//  HistoryListView.swift
//  QimenDunjia
//

import SwiftUI
import SwiftData

struct HistoryListView: View {
    @Query(sort: \HistoryRecord.createdAt, order: .reverse) private var records: [HistoryRecord]
    @Environment(\.modelContext) private var modelContext

    var body: some View {
        NavigationStack {
            Group {
                if records.isEmpty {
                    ContentUnavailableView {
                        Label("暂无记录", systemImage: "clock")
                    } description: {
                        Text("在「起局」完成排盘后，记录会保存在本机。")
                    }
                } else {
                    List {
                        ForEach(records) { record in
                            if let chart = record.decodedChart() {
                                NavigationLink {
                                    ChartDetailView(chart: chart)
                                } label: {
                                    VStack(alignment: .leading, spacing: 4) {
                                        if !record.question.isEmpty {
                                            Text(record.question)
                                                .font(.system(size: 16, weight: .medium, design: .serif))
                                                .foregroundStyle(AppTheme.ink)
                                                .lineLimit(2)
                                            Text(chart.juTitle)
                                                .font(.subheadline)
                                                .foregroundStyle(AppTheme.muted)
                                        } else {
                                            Text(chart.juTitle)
                                                .font(.system(size: 16, weight: .medium, design: .serif))
                                                .foregroundStyle(AppTheme.ink)
                                        }
                                        Text("\(chart.hourSB.name)时 · \(chart.zhiFuStar.shortName)/\(chart.zhiShiGate.displayName)")
                                            .font(.subheadline)
                                            .foregroundStyle(AppTheme.muted)
                                        Text(record.queryDate.formatted(date: .abbreviated, time: .shortened))
                                            .font(.caption)
                                            .foregroundStyle(AppTheme.muted.opacity(0.85))
                                    }
                                    .padding(.vertical, 2)
                                }
                                .listRowBackground(AppTheme.paper.opacity(0.6))
                            }
                        }
                        .onDelete { indexSet in
                            for i in indexSet { modelContext.delete(records[i]) }
                        }
                    }
                    .scrollContentBackground(.hidden)
                }
            }
            .background(AppTheme.screenBackground)
            .navigationTitle("历史")
        }
    }
}
