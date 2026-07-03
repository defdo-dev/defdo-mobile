import Foundation

/// Client for POST /mobile/bootstrap — the authoritative app-state call.
///
/// Sends ONLY the bearer access token. It deliberately does NOT send
/// brand_code, brand_key, app_key, theme_code, or tenant as authority. The
/// backend derives app/brand/tenant context from the OAuth client behind the
/// token and returns it in the AccessContext.
public struct BootstrapClient: Sendable {
    private let http: HTTPClient
    private let endpoint: String

    public init(http: HTTPClient, endpoint: String) {
        self.http = http
        self.endpoint = endpoint
    }

    public func bootstrap(accessToken: String) async -> BootstrapResult {
        let response: HTTPResponse
        do {
            response = try await http.request(
                method: "POST",
                url: endpoint,
                headers: [
                    "Authorization": "Bearer \(accessToken)",
                    "Accept": "application/json",
                    "X-Defdo-Platform": "ios"
                ],
                body: "{}"
            )
        } catch {
            return .networkError(error.localizedDescription)
        }

        switch response.status {
        case 200:
            return parseOK(response.body)
        case 401:
            return .unauthorized
        case 403:
            return .forbidden("Your account is not allowed to use this app.")
        case 500...599:
            return .networkError("server error \(response.status)")
        default:
            return .malformedResponse("unexpected status \(response.status)")
        }
    }

    private func parseOK(_ body: String) -> BootstrapResult {
        guard let data = body.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return .malformedResponse("invalid bootstrap body")
        }
        guard let status = json["status"] as? String else {
            return .malformedResponse("missing status")
        }
        let context = parseContext(json)
        switch status {
        case "ready":
            return .ready(context)
        case "needs_line_linking":
            return .needsLineLinking(context)
        default:
            return .malformedResponse("unknown status: \(status)")
        }
    }

    private func parseContext(_ json: [String: Any]) -> AccessContext {
        let brand = json["brand"] as? [String: Any]
        let app = json["app"] as? [String: Any]
        let tenant = json["tenant"] as? [String: Any]
        let theme = json["theme"] as? [String: Any]
        return AccessContext(
            brandKey: brand?["key"] as? String,
            appKey: app?["key"] as? String,
            tenantCode: tenant?["code"] as? String,
            themeEndpoint: theme?["endpoint"] as? String
        )
    }
}
