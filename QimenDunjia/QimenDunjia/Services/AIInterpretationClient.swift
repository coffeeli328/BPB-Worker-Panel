//
//  AIInterpretationClient.swift
//  QimenDunjia
//
//  OpenAI-compatible Chat Completions HTTP client. No hosted backend.
//

import Foundation

enum AIInterpretationError: LocalizedError {
    case notConfigured
    case invalidURL
    case httpStatus(Int, String)
    case emptyContent
    case network(Error)

    var errorDescription: String? {
        switch self {
        case .notConfigured:
            return "尚未配置 API Key。请到「设置」填写 Base URL、模型与密钥。"
        case .invalidURL:
            return "Base URL 无效。示例：https://api.deepseek.com/v1 或 https://api.openai.com/v1"
        case .httpStatus(let code, let body):
            let snippet = body.prefix(180)
            return "服务返回 \(code)：\(snippet)"
        case .emptyContent:
            return "模型未返回有效文本，请重试或更换模型。"
        case .network(let err):
            return "网络错误：\(err.localizedDescription)。可继续使用本机规则解读。"
        }
    }
}

struct AIInterpretationClient {
    struct Config: Sendable {
        var baseURL: String
        var apiKey: String
        var model: String
    }

    private let session: URLSession

    init(session: URLSession = .shared) {
        self.session = session
    }

    static func normalizedChatURL(baseURL: String) -> URL? {
        var b = baseURL.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !b.isEmpty else { return nil }
        while b.hasSuffix("/") { b.removeLast() }
        // Accept host-only DeepSeek style or already …/v1
        if !b.lowercased().hasSuffix("/v1") {
            b += "/v1"
        }
        return URL(string: b + "/chat/completions")
    }

    func interpret(chart: QimenChart, config: Config) async throws -> String {
        let key = config.apiKey.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !key.isEmpty else { throw AIInterpretationError.notConfigured }
        guard let url = Self.normalizedChatURL(baseURL: config.baseURL) else {
            throw AIInterpretationError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(key)", forHTTPHeaderField: "Authorization")
        request.timeoutInterval = 90

        let body: [String: Any] = [
            "model": config.model,
            "temperature": 0.4,
            "messages": [
                ["role": "system", "content": AIInterpretationPrompt.systemMessage],
                ["role": "user", "content": AIInterpretationPrompt.userPayload(chart: chart)]
            ]
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch {
            throw AIInterpretationError.network(error)
        }

        let code = (response as? HTTPURLResponse)?.statusCode ?? 0
        guard (200...299).contains(code) else {
            let text = String(data: data, encoding: .utf8) ?? ""
            throw AIInterpretationError.httpStatus(code, text)
        }

        guard
            let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
            let choices = json["choices"] as? [[String: Any]],
            let first = choices.first,
            let message = first["message"] as? [String: Any],
            let content = message["content"] as? String
        else {
            throw AIInterpretationError.emptyContent
        }

        let trimmed = content.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { throw AIInterpretationError.emptyContent }
        return trimmed
    }
}
