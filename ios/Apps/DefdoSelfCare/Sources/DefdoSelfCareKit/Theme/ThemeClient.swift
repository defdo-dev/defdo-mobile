import Foundation
import DefdoThemeMobile

public enum ThemeFetchResult: Sendable {
    case fresh(tokens: ThemeTokens, body: String, etag: String?)
    case notModified
    case unauthorized
    case forbidden(String)
    case notFound(String)
    case unavailable(String)
}

/// Client for GET /mobile/theme.
///
/// Sends ONLY the bearer token and an optional If-None-Match. It does NOT send
/// brand_code, brand_key, app_key, theme_code, or tenant as authority.
public struct ThemeClient: Sendable {
    private let http: HTTPClient
    private let endpoint: String

    public init(http: HTTPClient, endpoint: String) {
        self.http = http
        self.endpoint = endpoint
    }

    public func fetch(accessToken: String, etag: String?) async -> ThemeFetchResult {
        var headers = [
            "Authorization": "Bearer \(accessToken)",
            "Accept": "application/json"
        ]
        if let etag, !etag.isEmpty {
            headers["If-None-Match"] = etag
        }

        let response: HTTPResponse
        do {
            response = try await http.request(method: "GET", url: endpoint, headers: headers, body: nil)
        } catch {
            return .unavailable(error.localizedDescription)
        }

        switch response.status {
        case 200:
            guard let tokens = ThemeCodec.parse(response.body) else {
                return .unavailable("invalid theme body")
            }
            return .fresh(tokens: tokens, body: response.body, etag: response.header("ETag"))
        case 304:
            return .notModified
        case 401:
            return .unauthorized
        case 403:
            return .forbidden("theme forbidden (403)")
        case 404:
            return .notFound("theme not found (404)")
        default:
            return .unavailable("unexpected status \(response.status)")
        }
    }
}
