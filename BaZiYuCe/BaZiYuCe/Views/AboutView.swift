//
//  AboutView.swift
//  BaZiYuCe
//

import SwiftUI

struct AboutView: View {
    var body: some View {
        NavigationStack {
            ZStack {
                AppTheme.screenBackground
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        Text("八字运势")
                            .font(AppTheme.brandFont)
                            .foregroundStyle(AppTheme.ink)

                        Text("本地四柱排盘与规则化流年 / 流月 / 流日提示。预测文案由十神、地支关系与五行中和规则生成，可在 `PredictionEngine` 中扩展。")
                            .font(AppTheme.bodyFont)
                            .foregroundStyle(AppTheme.slate)

                        group(title: "计算口径") {
                            Text("· 年柱：立春换年\n· 月柱：十二节换月（五虎遁）\n· 日柱：儒略日干支；子时换日\n· 时柱：五鼠遁")
                        }

                        group(title: "残差说明") {
                            Text("节气交节时刻采用 Meeus 低精度太阳黄经近似，1900–2100 通常与精密历差数分钟；交节前后极短窗口可能与专业历书差一日。本 App 供学习与参考，不构成决策建议。")
                        }

                        group(title: "工程") {
                            Text("路径：`BaZiYuCe/BaZiYuCe.xcodeproj`\n需要 macOS + Xcode 15+，iOS 17+。签名与 Bundle ID 请在本机设置。")
                        }
                    }
                    .padding(20)
                }
            }
            .navigationTitle("关于")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private func group(title: String, @ViewBuilder content: () -> Text<String>) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(AppTheme.sectionFont)
                .foregroundStyle(AppTheme.celadonSoft)
            content()
                .font(AppTheme.bodyFont)
                .foregroundStyle(AppTheme.ink)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(.white.opacity(0.45))
        )
    }
}
