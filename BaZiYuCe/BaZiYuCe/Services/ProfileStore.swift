//
//  ProfileStore.swift
//  BaZiYuCe
//
//  UserDefaults 持久化多人档案。
//

import Foundation

struct ProfileStorePayload: Codable {
    var profiles: [PersonProfile]
    var activeProfileId: UUID?
}

enum ProfileStore {
    private static let key = "BaZiYuCe.profiles.v1"

    static func load() -> ProfileStorePayload {
        guard let data = UserDefaults.standard.data(forKey: key),
              let payload = try? JSONDecoder().decode(ProfileStorePayload.self, from: data)
        else {
            return ProfileStorePayload(profiles: [], activeProfileId: nil)
        }
        return payload
    }

    static func save(_ payload: ProfileStorePayload) {
        guard let data = try? JSONEncoder().encode(payload) else { return }
        UserDefaults.standard.set(data, forKey: key)
    }
}
