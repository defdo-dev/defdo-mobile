import Foundation

/// Minimal HTTP abstraction so coordinators stay testable with fakes.
/// Real platform transport (URLSessionHTTPClient) lives alongside it.
public protocol HTTPClient: Sendable {
    func request(
        method: String,
        url: String,
        headers: [String: String],
        body: String?
    ) async throws -> HTTPResponse
}

public struct HTTPResponse: Sendable {
    public let status: Int
    public let headers: [String: String]
    public let body: String

    public init(status: Int, headers: [String: String], body: String) {
        self.status = status
        self.headers = headers
        self.body = body
    }

    /// Case-insensitive header lookup.
    public func header(_ name: String) -> String? {
        for (key, value) in headers where key.caseInsensitiveCompare(name) == .orderedSame {
            return value
        }
        return nil
    }
}
