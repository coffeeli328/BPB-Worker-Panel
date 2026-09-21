//
//  ArticleRowView.swift
//  TopicRadar
//

import SwiftUI

struct ArticleRowView: View {
    let article: RadarArticle

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let url = article.url {
                Link(destination: url) {
                    Text(article.title)
                        .font(.headline)
                        .foregroundStyle(AppTheme.ink)
                        .multilineTextAlignment(.leading)
                }
            } else {
                Text(article.title)
                    .font(.headline)
                    .foregroundStyle(AppTheme.ink)
            }

            HStack(spacing: 8) {
                Text(article.sourceName)
                Text("·")
                Text((article.publishedAt ?? article.collectedAt).formatted(date: .abbreviated, time: .shortened))
            }
            .font(.caption)
            .foregroundStyle(AppTheme.muted)

            if !article.summary.isEmpty {
                Text(article.summary)
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.muted)
                    .lineLimit(4)
            }

            if !article.matchedKeywords.isEmpty {
                HStack(spacing: 6) {
                    ForEach(article.matchedKeywords.prefix(6), id: \.self) { keyword in
                        Text(keyword)
                            .font(.caption2)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(AppTheme.seaBright.opacity(0.22))
                            .foregroundStyle(AppTheme.sea)
                            .clipShape(Capsule())
                    }
                }
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.white.opacity(0.78))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(AppTheme.line, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}
