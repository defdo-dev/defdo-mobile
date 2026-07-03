# Defdo Mobile Architecture

Status: active (Milestone 2-F complete)
Scope: Auth SDK compile and contract readiness complete; live device verification pending manual run

## A. Executive Summary

Defdo Mobile uses **Native Dual Codebases + Shared Contracts**.

There are two native codebases coordinated by shared contracts:

- Android: Kotlin + Jetpack Compose.
- iOS: Swift + SwiftUI.

There is no Rust, no Kotlin Multiplatform, and no shared runtime core. Shared
means contracts, schemas, fixtures, test vectors, and CI validation only.

At this stage, only two reusable native mobile modules matter:

- Auth
- Theme

Do not add reusable self-care API, diagnostics, support, observability, or
storage packages yet. Those concerns may exist only as internal interfaces
needed by Auth or Theme.

## B. Recommended Mobile Technology Stack

Android:

- Kotlin.
- Jetpack Compose for future app and theme integration.
- Android App Links for auth callbacks.
- Android Keystore-backed secure storage through app-provided adapter.
- Android Gradle modules:
  - `:android:modules:auth`
  - `:android:modules:theme`

iOS:

- Swift.
- SwiftUI for future app and theme integration.
- Universal Links for auth callbacks.
- Keychain-backed secure storage through app-provided adapter.
- Swift Packages:
  - `DefdoAuthMobile`
  - `DefdoThemeMobile`

Shared artifacts only:

- auth contracts
- theme contracts
- brand manifest schema
- fixtures
- PKCE test vectors
- OpenAPI specs when a mobile BFF exists later
- semantic theme token schemas
- validation tools

## C. Repository Structure

```text
defdo_mobile/
  android/
    apps/
      dev-auth-harness/
      defdo-selfcare/         # first branded app shell
    modules/
      auth/
      auth-android/
      theme/
  ios/
    Apps/
      DevAuthHarness/
      DefdoSelfCare/          # first branded app shell (DefdoSelfCareKit + exe)
    Packages/
      DefdoAuthMobile/
        Sources/DefdoAuthMobile/
          Core/
          Platform/
        Tests/DefdoAuthMobileTests/
      DefdoThemeMobile/
  shared-contracts/
    auth/
    theme/
    brand/
  tools/
    validate-auth-contract/
    validate-theme-contract/
    validate-brand-manifest/
  docs/
    mobile_architecture.md
    auth_dev_harness_runbook.md
    auth_live_smoke_checklist.md
```

## D. Reusable Module Rule

Native dual codebases does not mean every branded app reimplements login or
theme.

Each branded Android app imports:

- `:android:modules:auth`
- `:android:modules:theme`

Each branded iOS app imports:

- `DefdoAuthMobile`
- `DefdoThemeMobile`

Branded apps are thin shells. They provide:

- app identity
- generated brand config
- bundle/application identifiers
- OAuth `client_id`
- redirect URI
- associated domains / app links
- build-time assets
- runtime endpoints
- environment
- feature flags

Reusable modules provide only Auth and Theme behavior for Milestone 1.

## E. Android Module Names

Android Gradle modules:

- `:android:modules:auth`
- `:android:modules:theme`

Future Maven-style coordinates:

- `dev.defdo.mobile:auth`
- `dev.defdo.mobile:theme`

Public package namespaces:

- `dev.defdo.mobile.auth`
- `dev.defdo.mobile.theme`

## F. iOS Package Names

Swift Packages:

- `DefdoAuthMobile`
- `DefdoThemeMobile`

Branded iOS apps import those packages.

## G. Auth Architecture

Auth is native per platform, contract-aligned across platforms.

Android:

- `DefdoAuthMobileClient` in package `dev.defdo.mobile.auth`.

iOS:

- `DefdoAuthMobileClient` in Swift package `DefdoAuthMobile`.

Both clients must implement:

- Authorization Code + PKCE orchestration
- discovery parsing
- auth request construction
- browser login handoff
- callback validation
- token exchange contract handling
- refresh handling
- revoke/logout handling
- secure token storage orchestration
- safe auth error normalization

The clients use platform-native browser/session adapters:

- Android: Chrome Custom Tabs or AppAuth-Android adapter.
- iOS: `ASWebAuthenticationSession` or AppAuth-iOS adapter.

Do not use embedded web views. Do not embed client secrets. Mobile
`client_id` is public metadata from the brand manifest.

`defdo_auth_client` remains the oracle/reference for expected OAuth/OIDC
behavior, but it is not the runtime mobile SDK. The mobile repo consumes
fixtures and contracts generated from or aligned with `defdo_auth_client`.

### Current auth module structure

Android:

```
android/modules/auth/                     # JVM pure auth core
  build.gradle.kts
  src/main/kotlin/dev/defdo/mobile/auth/
    (all core logic + interfaces + impl + fake adapters + token store)
  src/test/kotlin/dev/defdo/mobile/auth/
    (19 contract/behavior tests)

android/modules/auth-android/             # Android platform adapters
  build.gradle.kts (com.android.library, compileSdk 34, minSdk 26)
  src/main/kotlin/dev/defdo/mobile/auth/android/
    ChromeCustomTabsBrowserAdapter.kt
    AndroidKeystoreSecureStorageAdapter.kt
    AppLinkCallbackHandler.kt

android/apps/dev-auth-harness/            # Dev auth APK
  build.gradle.kts (com.android.application)
  src/main/ (Activity, layout, manifest)
```

iOS:

```
ios/Packages/DefdoAuthMobile/
  Package.swift
  Sources/DefdoAuthMobile/
    Core/                                 # Pure auth logic
      (DefdoAuthMobileClient, AuthConfig, AuthSession, PKCE,
       AuthorizationRequestBuilder, CallbackValidator,
       OAuthDiscoveryDocument, TokenResponse, RefreshResponse,
       RevokeResult, TokenStore, SecureTokenStore,
       SecureStorageAdapter, BrowserAuthAdapter, TokenHttpTransport,
       AuthError, AuthErrorNormalizer, AuthRedactor,
       DefdoAuthMobileClientImpl, InMemorySecureStorageAdapter,
       LoginRequest, LoginResult)
    Platform/                             # Platform adapters
      ASWebAuthenticationSessionBrowserAdapter.swift
      KeychainSecureStorageAdapter.swift
      UniversalLinkCallbackHandler.swift
      URLSessionTokenHttpTransport.swift
  Tests/DefdoAuthMobileTests/
    (63 XCTest + dev smoke harness)

ios/Apps/DevAuthHarness/                  # Dev harness SwiftPM package
```

## I. Auth Shared Contracts

Auth contracts live in `shared-contracts/auth/`:

- `defdo_auth_mobile_contract.json`
- `pkce.test_vectors.json`
- `discovery.success.fixture.json`
- `discovery.invalid.fixture.json`
- `authorization_request.expected.json`
- `token.success.fixture.json`
- `token.error.fixture.json`
- `refresh.success.fixture.json`
- `refresh.invalid_grant.fixture.json`
- `revoke.success.fixture.json`
- `revoke.failure.fixture.json`
- `auth_error_normalization.json`
- `auth_redaction_rules.json`

Both Android and iOS auth modules must pass equivalent contract tests against
the same fixtures.

Required auth contract behavior:

1. PKCE verifier format.
2. PKCE S256 challenge generation.
3. Authorization request includes `response_type=code`, `client_id`,
   `redirect_uri`, `scope`, `state`, `code_challenge`,
   `code_challenge_method=S256`, and `nonce` when configured.
4. Callback validator rejects missing code, missing state, mismatched state,
   OAuth error callback, and wrong redirect URI.
5. Discovery parser rejects missing required endpoints.
6. Token response parser supports success and normalized error responses.
7. Refresh flow replaces refresh token when issuer returns a new one.
8. `invalid_grant` clears local session and requires login.
9. Revoke failure still clears local session.
10. Redactor removes access tokens, refresh tokens, ID tokens, auth code, PKCE
    verifier, raw claims, and raw callback URLs with sensitive query params.

## J. Theme Architecture

Theme is native per platform, contract-aligned across platforms.

Android:

- `DefdoThemeClient` in package `dev.defdo.mobile.theme`.
- Maps Defdo semantic tokens into Compose theme/components.

iOS:

- `DefdoThemeClient` in Swift package `DefdoThemeMobile`.
- Maps Defdo semantic tokens into SwiftUI theme/components.

The shared part of theming is only:

- token schema
- semantic naming
- validation fixtures
- expected behavior

`defdo_theme_hub` remains the source of truth for build-time assets and runtime
semantic theme tokens. Theme Hub must not become a UI CMS.

Theme Hub may provide:

- app icons
- splash assets
- logos
- store assets
- fallback theme snapshots
- runtime semantic tokens
- theme versions
- accessibility metadata

Theme Hub must not provide:

- screen layouts
- navigation
- business logic
- copy
- arbitrary component trees

## K. Theme Module Scope

Android theme skeleton:

```text
android/modules/theme/
  build.gradle.kts
  src/main/kotlin/dev/defdo/mobile/theme/
    DefdoThemeClient.kt
    ThemeConfig.kt
    ThemeMode.kt
    ThemeTokens.kt
    ThemeTokenValidator.kt
    ThemeRepository.kt
    ThemeCache.kt
    ThemeTransport.kt
    ThemeAdapter.kt
    ComposeThemeAdapter.kt
    ThemeError.kt
  src/test/kotlin/dev/defdo/mobile/theme/
    ThemeTokenValidatorTest.kt
    ThemeFallbackTest.kt
    ThemeContractTest.kt
    ThemeRedactionTest.kt
```

iOS theme skeleton:

```text
ios/Packages/DefdoThemeMobile/
  Package.swift
  Sources/DefdoThemeMobile/
    DefdoThemeClient.swift
    ThemeConfig.swift
    ThemeMode.swift
    ThemeTokens.swift
    ThemeTokenValidator.swift
    ThemeRepository.swift
    ThemeCache.swift
    ThemeTransport.swift
    ThemeAdapter.swift
    SwiftUIThemeAdapter.swift
    ThemeError.swift
  Tests/DefdoThemeMobileTests/
    ThemeTokenValidatorTests.swift
    ThemeFallbackTests.swift
    ThemeContractTests.swift
    ThemeRedactionTests.swift
```

## L. Theme Shared Contracts

Theme contracts live in `shared-contracts/theme/`:

- `theme_token_schema.json`
- `fallback_theme.light.fixture.json`
- `fallback_theme.dark.fixture.json`
- `theme.success.fixture.json`
- `theme.missing_required_token.fixture.json`
- `theme.invalid_color.fixture.json`
- `theme.low_contrast.fixture.json`
- `theme_redaction_rules.json`

Theme rules:

- Build-time assets are fetched by CI.
- Runtime colors and semantic tokens are fetched by the app.
- The app boots with embedded fallback theme.
- Last-known-good valid theme wins over fallback.
- Invalid remote theme must be rejected.
- Remote theme failure must not block app boot.
- Theme Hub must not become a UI CMS.
- Theme Hub must not provide screen layouts, navigation, business logic, copy,
  or arbitrary component trees.

## M. Theme Transport

Today themes are distributed through GraphQL. Do not hardcode GraphQL as the
permanent architecture.

Model the Theme module around a transport abstraction:

- Android: `ThemeTransport` interface.
- iOS: `ThemeTransport` protocol.

A GraphQL implementation can exist now if needed, but the Theme module contract
must allow replacing it later with a stronger mobile REST/HTTP endpoint backed
by `defdo_theme_hub`.

Mobile consumes a stable mobile-facing theme contract.

## N. Brand Contracts

Brand contracts live in `shared-contracts/brand/`:

- `brand_manifest_schema.json`
- `sample.defdo-telecom.dev.manifest.json`

The brand manifest must include:

- `brand_key`
- `app_key`
- `environment`
- Android `applicationId`
- iOS bundle ID
- display name
- OAuth `client_id`
- OAuth discovery URL
- OAuth redirect URI
- associated domains / app links
- theme endpoint
- asset endpoint
- fallback theme version

## O. Validation Tools

Validation tools:

- `tools/validate-auth-contract/`
- `tools/validate-theme-contract/`
- `tools/validate-brand-manifest/`

They must validate:

- required files exist
- JSON parses
- schemas validate fixtures
- required auth fixture fields exist
- required theme tokens exist
- brand manifest has required platform/auth/theme fields

## P. Current Status (post Milestone 2-F)

### Auth SDK

- **Compile and contract readiness:** complete
- **Unit/contract tests:** 19 Android (JUnit + fallback runner), 63 iOS (XCTest)
- **Live device auth verification:** pending manual run (see `docs/auth_live_smoke_checklist.md`)
- **Runbook:** `docs/auth_dev_harness_runbook.md`

Auth module structure (actual):

```
android/modules/auth/          # JVM auth core (pure logic + interfaces)
android/modules/auth-android/  # Android platform adapters (real)
android/apps/dev-auth-harness/ # Dev auth APK
ios/Packages/DefdoAuthMobile/  # SwiftPM: Core/ + Platform/
ios/Apps/DevAuthHarness/       # SwiftPM dev harness
```

### Theme SDK

- **Work may begin in parallel**
- Auth live smoke remains a release gate before production deploy
- Theme work does not depend on live auth verification

### Known gates before release

1. Live device auth smoke must pass (see checklist)
2. App Links / Universal Links must be verified on issuer domain
3. Secure storage must be verified on real devices

## Q. Milestone Progression

| Milestone | Status |
|-----------|--------|
| 2-A: Pure auth logic + contract tests | Complete |
| 2-A hardening: encoding/discovery/redaction fixes | Complete |
| 2-B: Platform adapter boundaries + fake adapters | Complete |
| 2-C: auth-android module + token envelope + Gradle tests | Complete |
| 2-D: Live dev issuer smoke + issuer consistency + token_type | Complete |
| 2-E: Full manual dev login harness (APK + iOS app) | Complete |
| 2-F: Live device auth verification prep + runbook + checklist | Complete |
| 3-A: Theme module (next) | Not started |

## R. Validation Commands (current)

Deliver only:

- Android auth module skeleton.
- Android theme module skeleton.
- iOS auth Swift Package skeleton.
- iOS theme Swift Package skeleton.
- `shared-contracts/auth` fixtures.
- `shared-contracts/theme` fixtures.
- `shared-contracts/brand` schema and sample manifest.
- tools validators.
- minimal tests proving fixtures are loaded and contracts are enforced.
- documentation update.

Do not implement:

- UI screens.
- real OAuth network login.
- real token exchange.
- real Theme Hub network fetch.
- real GraphQL integration.
- BFF endpoints.
- diagnostics.
- Rust.
- Kotlin Multiplatform.
- shared runtime code.

## R. Validation Commands (current)

```sh
# Contracts
tools/validate-brand-manifest/validate
tools/validate-auth-contract/validate
tools/validate-theme-contract/validate

# iOS
swift test --package-path ios/Packages/DefdoAuthMobile
swift build --package-path ios/Apps/DevAuthHarness

# Android
./gradlew :android:modules:auth:test
./gradlew :android:modules:auth:runContractTests
./gradlew :android:modules:auth-android:compileDebugKotlin
./gradlew :android:apps:dev-auth-harness:assembleDebug
```

## S. Rollback

Rollback for auth is file-only:

```sh
rm -rf android/modules/auth android/modules/auth-android \
  android/apps/dev-auth-harness \
  ios/Packages/DefdoAuthMobile ios/Apps/DevAuthHarness \
  shared-contracts/auth docs/auth_*.md
```

No database migrations, backend services, or remote state are touched.
