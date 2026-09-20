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
                    ContentUnavailableView("暂无记录", systemImage: "clock", description: Text("起局后将自动保存在本地"))
                } else {
                    List {
                        ForEach(records) { record in
                            if let chart = record.decodedChart() {
                                NavigationLink {
                                    ChartDetailView(chart: chart)
                                } label: {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(record.summary)
                                        Text(record.queryDate.formatted(date: .abbreviated, time: .shortened))
                                            .font(.caption)
                                            .foregroundStyle(AppTheme.muted)
                                    }
                                }
                            }
                        }
                        .onDelete { indexSet in
                            for i in indexSet { modelContext.delete(records[i]) }
                        }
                    }
                }
            }
            .background(AppTheme.paper)
            .navigationTitle("历史")
        }
    }
}
