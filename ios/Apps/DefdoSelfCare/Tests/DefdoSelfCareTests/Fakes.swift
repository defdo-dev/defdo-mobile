import Foundation
@testable import DefdoSelfCareKit

struct RecordedRequest: Sendable {
    let method: String
    let url: String
    let headers: [String: String]
    let body: String?
}

/// Records every request so tests can assert the app never sends brand_code,
/// brand_key, app_key, theme_code, or tenant as authority — and never logs
/// tokens.
actor FakeHTTPClient: HTTPClient {
    private(set) var requests: [RecordedRequest] = []
    private var pendingThrow = false
    private let responder: @Sendable (RecordedRequest) -> HTTPResponse

    init(responder: @escaping @Sendable (RecordedRequest) -> HTTPResponse) {
        self.responder = responder
    }

    func setThrowOnNext() { pendingThrow = true }

    func request(method: String, url: String, headers: [String: String], body: String?) async throws -> HTTPResponse {
        let recorded = RecordedRequest(method: method, url: url, headers: headers, body: body)
        requests.append(recorded)
        if pendingThrow {
            pendingThrow = false
            throw URLError(.notConnectedToInternet)
        }
        return responder(recorded)
    }
}

actor FakeSessionProvider: SessionProvider {
    private var token: String?
    private(set) var clearCount = 0

    init(token: String?) { self.token = token }

    func currentAccessToken() async -> String? { token }

    func clear() async {
        clearCount += 1
        token = nil
    }
}
