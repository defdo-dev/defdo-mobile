import XCTest
@testable import DefdoSelfCareKit

final class AppConfigTests: XCTestCase {

    func testFromEnvironmentUsesDefaultsWhenUnset() {
        let config = AppConfig.fromEnvironment([:])

        XCTAssertEqual(config.issuer, "")
        XCTAssertEqual(config.discoveryURL, "")
        XCTAssertEqual(config.clientID, "defdo-telecom-mobile-dev")
        XCTAssertEqual(config.redirectURI, "https://login.defdo-telecom.example/mobile/oauth/callback")
        XCTAssertEqual(config.scopes, ["openid", "profile", "offline_access"])
        XCTAssertEqual(config.bootstrapEndpoint, "https://api.defdo.example/mobile/bootstrap")
        XCTAssertEqual(config.themeEndpoint, "https://api.defdo.example/mobile/theme")
        XCTAssertEqual(config.environment, "dev")
        XCTAssertFalse(config.isConfigured)
    }

    func testFromEnvironmentBuildsDiscoveryURLFromIssuer() {
        let config = AppConfig.fromEnvironment(["DEFDO_DEV_ISSUER": "https://auth.example"])

        XCTAssertEqual(config.discoveryURL, "https://auth.example/.well-known/openid-configuration")
        XCTAssertTrue(config.isConfigured)
    }

    func testFromEnvironmentCanOverrideRedirectURI() {
        let config = AppConfig.fromEnvironment(["DEFDO_DEV_REDIRECT_URI": "https://login.example/app/callback"])

        XCTAssertEqual(config.redirectURI, "https://login.example/app/callback")
        XCTAssertFalse(config.usesCustomScheme)
    }

    func testCustomSchemeRedirectIsDetected() {
        let config = AppConfig.fromEnvironment(["DEFDO_DEV_REDIRECT_URI": "defdo.selfcare.dev://oauth.callback/"])

        XCTAssertTrue(config.usesCustomScheme)
    }

    func testFromBundleReadsInfoDictionary() {
        let info: [String: Any] = [
            "DEFDO_DEV_ISSUER": "https://auth.example",
            "DEFDO_DEV_CLIENT_ID": "client-from-bundle",
            "DEFDO_DEV_REDIRECT_URI": "https://login.example/callback",
            "DEFDO_DEV_SCOPES": "openid profile",
            "DEFDO_BACKEND_BASE_URL": "https://bff.example",
            "DEFDO_ENVIRONMENT": "local"
        ]
        let config = AppConfig.fromBundle(info)

        XCTAssertEqual(config.issuer, "https://auth.example")
        XCTAssertEqual(config.discoveryURL, "https://auth.example/.well-known/openid-configuration")
        XCTAssertEqual(config.clientID, "client-from-bundle")
        XCTAssertEqual(config.redirectURI, "https://login.example/callback")
        XCTAssertEqual(config.scopes, ["openid", "profile"])
        XCTAssertEqual(config.bootstrapEndpoint, "https://bff.example/mobile/bootstrap")
        XCTAssertEqual(config.themeEndpoint, "https://bff.example/mobile/theme")
        XCTAssertEqual(config.environment, "local")
    }

    func testFromBundleFallsBackToEnvironment() {
        let info: [String: Any] = [:]
        let env = [
            "DEFDO_DEV_ISSUER": "https://env.example",
            "DEFDO_DEV_CLIENT_ID": "env-client"
        ]
        let config = AppConfig.fromBundle(info, environment: env)

        XCTAssertEqual(config.issuer, "https://env.example")
        XCTAssertEqual(config.clientID, "env-client")
    }

    func testRedirectURIHelperReconstructsHTTPSCallback() {
        let config = AppConfig.fromEnvironment([:])
        let uri = config.redirectURI(scheme: "https", host: "login.example", path: "/mobile/oauth/callback")

        XCTAssertEqual(uri, "https://login.example/mobile/oauth/callback")
    }

    func testRedirectURIHelperNormalizesLeadingSlash() {
        let config = AppConfig.fromEnvironment([:])
        let uri = config.redirectURI(scheme: "defdo.selfcare.dev", host: "oauth.callback", path: "complete")

        XCTAssertEqual(uri, "defdo.selfcare.dev://oauth.callback/complete")
    }

    func testEndpointsDoNotCarryAuthorityParameters() {
        let config = AppConfig.fromEnvironment([:])
        let endpoints = [config.bootstrapEndpoint, config.themeEndpoint]
        let forbidden = ["brand_code", "brand_key", "app_key", "theme_code", "tenant"]

        for endpoint in endpoints {
            let lower = endpoint.lowercased()
            for term in forbidden {
                XCTAssertFalse(lower.contains(term), "endpoint must not carry '\(term)' as authority: \(endpoint)")
            }
        }
    }
}
