import Foundation

/// AccessContext is the backend's answer for "who/what is this token allowed to
/// act as" — brand/app/tenant come FROM here, never sent BY the app.
public struct AccessContext: Sendable, Equatable {
    public let brandKey: String?
    public let appKey: String?
    public let tenantCode: String?
    public let themeEndpoint: String?

    public init(brandKey: String?, appKey: String?, tenantCode: String?, themeEndpoint: String?) {
        self.brandKey = brandKey
        self.appKey = appKey
        self.tenantCode = tenantCode
        self.themeEndpoint = themeEndpoint
    }
}

public enum BootstrapResult: Sendable, Equatable {
    case ready(AccessContext)
    case needsLineLinking(AccessContext)
    case unauthorized
    case forbidden(String)
    case networkError(String)
    case malformedResponse(String)
}
