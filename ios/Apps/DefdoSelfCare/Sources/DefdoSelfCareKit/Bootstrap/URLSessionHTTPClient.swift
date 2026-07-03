import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

/// URLSession-backed HTTP transport. Never logs the Authorization header or
/// response bodies.
public struct URLSessionHTTPClient: HTTPClient {
    private let session: URLSession

    public init(session: URLSession = .shared) {
        self.session = session
    }

    public func request(
        method: String,
        url: String,
        headers: [String: String],
        body: String?
    ) async throws -> HTTPResponse {
        guard let requestURL = URL(string: url) else {
            throw URLError(.badURL)
        }
        var request = URLRequest(url: requestURL)
        request.httpMethod = method
        request.timeoutInterval = 10
        for (key, value) in headers { request.setValue(value, forHTTPHeaderField: key) }
        if let body {
            request.httpBody = body.data(using: .utf8)
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }
        var responseHeaders: [String: String] = [:]
        for (key, value) in http.allHeaderFields {
            if let k = key as? String, let v = value as? String { responseHeaders[k] = v }
        }
        let responseBody = String(data: data, encoding: .utf8) ?? ""
        return HTTPResponse(status: http.statusCode, headers: responseHeaders, body: responseBody)
    }
}
