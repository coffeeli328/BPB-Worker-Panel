//
//  AISettingsStore.swift
//  QimenDunjia
//
//  OpenAI-compatible endpoint settings. API key in Keychain; URL/model in UserDefaults.
//

import Foundation
import Combine

@MainActor
final class AISettingsStore: ObservableObject {
    static let shared = AISettingsStore()

    /// Example placeholders (not used until user saves).
    static let placeholderBaseURLDeepSeek = "https://api.deepseek.com/v1"
    static let placeholderBaseURLOpenAI = "https://api.openai.com/v1"
    static let placeholderModelDeepSeek = "deepseek-chat"
    static let placeholderModelOpenAI = "gpt-4o-mini"

    private enum Keys {
        static let baseURL = "ai.baseURL"
        static let model = "ai.model"
        static let keyAccount = "apiKey"
    }

    @Published var baseURL: String {
        didSet { UserDefaults.standard.set(baseURL, forKey: Keys.baseURL) }
    }

    @Published var model: String {
        didSet { UserDefaults.standard.set(model, forKey: Keys.model) }
    }

    /// In-memory mirror of Keychain; never logged.
    @Published var apiKey: String {
        didSet { KeychainStore.set(apiKey, account: Keys.keyAccount) }
    }

    private init() {
        self.baseURL = UserDefaults.standard.string(forKey: Keys.baseURL) ?? ""
        self.model = UserDefaults.standard.string(forKey: Keys.model) ?? ""
        self.apiKey = KeychainStore.get(account: Keys.keyAccount)
    }

    var isConfigured: Bool {
        !apiKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !resolvedBaseURL.isEmpty
            && !resolvedModel.isEmpty
    }

    /// Prefer saved values; fall back to DeepSeek-style defaults only when key is set but fields blank.
    var resolvedBaseURL: String {
        let t = baseURL.trimmingCharacters(in: .whitespacesAndNewlines)
        if !t.isEmpty { return t }
        return Self.placeholderBaseURLDeepSeek
    }

    var resolvedModel: String {
        let t = model.trimmingCharacters(in: .whitespacesAndNewlines)
        if !t.isEmpty { return t }
        return Self.placeholderModelDeepSeek
    }

    func applyDeepSeekPreset() {
        baseURL = Self.placeholderBaseURLDeepSeek
        model = Self.placeholderModelDeepSeek
    }

    func applyOpenAIPreset() {
        baseURL = Self.placeholderBaseURLOpenAI
        model = Self.placeholderModelOpenAI
    }

    func clearKey() {
        apiKey = ""
    }
}
