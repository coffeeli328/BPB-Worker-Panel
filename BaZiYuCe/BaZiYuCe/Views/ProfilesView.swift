//
//  ProfilesView.swift
//  BaZiYuCe
//
//  多人档案：切换、新增、重命名、删除。
//

import SwiftUI

struct ProfilesView: View {
    @Environment(BaZiSession.self) private var session
    @State private var renameTarget: PersonProfile?
    @State private var renameText = ""
    @State private var showRename = false

    var body: some View {
        NavigationStack {
            ZStack {
                AppTheme.screenBackground
                List {
                    Section {
                        Button {
                            session.startNewProfileDraft()
                        } label: {
                            Label("新建档案并起盘", systemImage: "plus.circle.fill")
                                .foregroundStyle(AppTheme.celadon)
                        }
                    } footer: {
                        Text("每位档案独立保存出生信息与排盘；切换后八字、运势、日历随之更换。")
                            .font(.footnote)
                    }

                    Section("已保存档案") {
                        if session.profiles.isEmpty {
                            Text("暂无档案，请先在「起盘」排出八字。")
                                .foregroundStyle(AppTheme.slate)
                        } else {
                            ForEach(session.profiles) { profile in
                                profileRow(profile)
                            }
                            .onDelete(perform: delete)
                        }
                    }

                    Section("关于") {
                        NavigationLink("计算口径与免责声明") {
                            AboutView(embedded: true)
                        }
                    }
                }
                .scrollContentBackground(.hidden)
            }
            .navigationTitle("档案")
            .navigationBarTitleDisplayMode(.inline)
            .alert("重命名档案", isPresented: $showRename) {
                TextField("名称", text: $renameText)
                Button("取消", role: .cancel) {}
                Button("保存") {
                    if let id = renameTarget?.id {
                        session.renameProfile(id, to: renameText)
                    }
                }
            } message: {
                Text("为档案设置便于识别的中文名")
            }
        }
    }

    private func profileRow(_ profile: PersonProfile) -> some View {
        let active = session.activeProfileId == profile.id
        return HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(profile.name)
                        .font(.headline)
                        .foregroundStyle(AppTheme.ink)
                    if active {
                        Text("当前")
                            .font(.caption2.weight(.semibold))
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(AppTheme.celadon.opacity(0.15))
                            .foregroundStyle(AppTheme.celadon)
                            .clipShape(Capsule())
                    }
                }
                Text(profile.summaryLine)
                    .font(.caption)
                    .foregroundStyle(AppTheme.slate)
            }
            Spacer()
            Menu {
                Button("切换到此档案") {
                    session.switchToProfile(profile.id)
                }
                Button("重命名") {
                    renameTarget = profile
                    renameText = profile.name
                    showRename = true
                }
                Button("删除", role: .destructive) {
                    session.deleteProfile(profile.id)
                }
            } label: {
                Image(systemName: "ellipsis.circle")
                    .foregroundStyle(AppTheme.celadon)
            }
        }
        .contentShape(Rectangle())
        .onTapGesture {
            session.switchToProfile(profile.id)
        }
        .listRowBackground(Color.white.opacity(0.45))
    }

    private func delete(at offsets: IndexSet) {
        for index in offsets {
            session.deleteProfile(session.profiles[index].id)
        }
    }
}
