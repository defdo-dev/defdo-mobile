# Defdo Mobile App Shell (defdo-selfcare / DefdoSelfCare)

Status: implemented (first app shell)
Scope: Real app target on Android + iOS consuming the existing Auth and Theme
mobile libraries, ready to consume `POST /mobile/bootstrap` and
`GET /mobile/theme`.

This is the first branded app shell. It does **not** implement line linking,
usage/consumption screens, or payments. It establishes startup, auth wiring,
secure token storage, bootstrap, layered theming, and the minimal screen set.

## A. App targets

| Platform | Target | Path |
|----------|--------|------|
| Android | `:android:apps:defdo-selfcare` | `android/apps/defdo-selfcare` |
| iOS | `DefdoSelfCare` (SwiftPM: `DefdoSelfCareKit` lib + `DefdoSelfCare` exe) | `ios/Apps/DefdoSelfCare` |

### Libraries consumed

- Android: `:android:modules:auth`, `:android:modules:auth-android`,
  `:android:modules:theme`
- iOS: `DefdoAuthMobile`, `DefdoThemeMobile`

App-local services (per platform, same names) layer on top of the reusable
libraries: `AppConfig`, `AuthSessionCoordinator`, `BootstrapClient`,
`ThemeClient`, `ThemeRepository`, `ThemeCache`, `EmbeddedTheme`,
`AppStartupCoordinator`.

## B. Authority model (read this first)

The single most important constraint:

> Mobile derives app/brand/tenant context from the configured OAuth client +
> the backend `AccessContext` returned by bootstrap. The app **never** sends
> `brand_code`, `brand_key`, `app_key`, `theme_code`, or `tenant` as authority.

- The only brand/app identity the app asserts is the OAuth `client_id`, and it
  is asserted to **defdo_auth** during the OAuth flow — not to the product
  backend.
- `POST /mobile/bootstrap` and `GET /mobile/theme` send only the bearer access
  token (plus `If-None-Match` for theme). The backend resolves brand/app/tenant
  from the OAuth client behind the token.
- The brand manifest (`shared-contracts/brand/*.manifest.json`) is build-time
  app identity. Its `brand_key`/`app_key` configure the build; they are never
  transmitted to the backend as authority.

Tests enforce this: `doesNotSendBrandAppTenantAsAuthority` (Android) /
`testDoesNotSendBrandAppTenantAsAuthority` (iOS) scan every outgoing request's
URL, headers, and body for those terms and fail if present.

## C. Startup flow

```
1. App starts.
2. Load embedded fallback theme immediately (no network).      [EmbeddedTheme]
3. Overlay last-known-good cached theme if present.            [ThemeCache]
4. Check token session from secure storage.                   [AuthSessionCoordinator]
5. No token        -> SignedOut screen; login uses auth lib browser flow.
6. Token present   -> bootstrapLoading -> POST /mobile/bootstrap.
7. In parallel (token present) -> GET /mobile/theme (If-None-Match).
```

Steps 2-3 are synchronous and local, so the shell renders a themed UI before any
network call. Step 7 runs concurrently with step 6 and never blocks navigation.

The `AppStartupCoordinator` is pure logic (no Android/SwiftUI imports) and is the
unit-tested heart of the flow on both platforms.

## D. Auth flow

- Uses the existing mobile auth library. PKCE, callback validation, token
  exchange, refresh, and revoke are **not reimplemented** — they live in
  `DefdoAuthMobileClient`.
- Tokens are stored only in platform secure storage:
  - Android: `AndroidKeystoreSecureStorageAdapter`
    (EncryptedSharedPreferences / Keystore) via `SecureTokenStore`.
  - iOS: `KeychainSecureStorageAdapter` via `SecureTokenStore`.
- `AuthSessionCoordinator` reads the access token through a narrow
  `SessionProvider` port (`AuthClientSessionProvider` wraps the real client),
  which keeps coordinators unit-testable and ensures tokens are never read from
  plain preferences/UserDefaults.
- Login uses the platform browser flow (Chrome Custom Tabs / ASWebAuthentication
  Session). The redirect is delivered via App Link / Universal Link and routed
  to the auth client's callback handler.

## E. Bootstrap flow

`POST /mobile/bootstrap`, `Authorization: Bearer <access_token>`.

| Result | App state |
|--------|-----------|
| 200 `ready` | `ReadyHome` (with `AccessContext`) |
| 200 `needs_line_linking` | `NeedsLineLinking` placeholder |
| 401 | clear auth session, `SignedOut` |
| 403 | `Error` (safe copy), **not retryable** (no infinite retry) |
| network error | `Error`, **retryable**, session kept |
| malformed body | `Error` (safe copy), not retryable |

`AccessContext` (`brandKey`, `appKey`, `tenantCode`, `themeEndpoint`) is parsed
**from** the bootstrap response and is the only source of brand/app/tenant in
the app.

## F. Theme flow

`GET /mobile/theme`, `Authorization: Bearer <token>`, `If-None-Match: <etag>`
when cached.

| Status | Behavior |
|--------|----------|
| 200 | parse + validate body, cache body+ETag, apply tokens |
| 304 | use cached body |
| 401 | clear session / return to signed out (app policy) |
| 403 | keep embedded/last-known-good; dev-only diagnostic |
| 404 | keep embedded/last-known-good; dev-only diagnostic |
| network/invalid | keep fallback; dev-only diagnostic |

### Layered theme (never blocks startup, never crashes)

1. **Embedded fallback** — `EmbeddedTheme`, always available, never expires.
   Mirrors `shared-contracts/theme/fallback_theme.{light,dark}.fixture.json`
   (Android also bundles them under `assets/`).
2. **Last-known-good cache** — validated disk cache wins over embedded.
3. **Fresh runtime theme** — applied when `/mobile/theme` returns 200.

`ThemeRepository.localTheme(mode)` returns the best non-network theme (cache or
embedded). `ThemeRepository.refresh(token, mode)` performs the conditional
fetch and applies the policy above. The app does **not** require the theme
backend to be released to run.

## G. Cache behavior

`ThemeCache` persists per mode: theme JSON `body`, `etag`, `fetched_at`,
`schema_version`, `theme_version`. Production impls are file-backed
(`FileThemeCache`); tests use `InMemoryThemeCache`.

Validation before applying / before trusting a cached body (`ThemeCodec` +
`ThemeTokenValidator` from the theme module):

- `schema_version == 1`
- `tokens` is a map of string→string
- all required color tokens present
- all color token values parse as `#RRGGBB`
- invalid cache is **discarded and deleted**; the app falls back to embedded.

## H. Error handling

- Bootstrap 403 and malformed responses produce a non-retryable `Error` screen
  with safe user copy (no raw server text, no tokens).
- Network errors keep the user signed in and offer Retry.
- Theme failures (403/404/network/invalid) never surface to end users in
  production; in dev builds a non-blocking diagnostic string is attached to the
  theme state (`BuildConfig.DEV_DIAGNOSTICS` on Android, `#if DEBUG` on iOS).
- Tokens are only ever placed in the `Authorization` header — never in URLs,
  bodies, or logs. The HTTP clients do not log headers or bodies. Auth-layer
  redaction (`AuthRedactor`) remains the source of truth for auth logs.

## I. Environment configuration

`AppConfig` resolves from environment variables with non-secret dev defaults.
No production credentials are hardcoded.

| Variable | Meaning | Dev default |
|----------|---------|-------------|
| `DEFDO_DEV_ISSUER` | defdo_auth issuer base URL | (empty → unconfigured) |
| `DEFDO_DEV_DISCOVERY_URL` | OIDC discovery URL | `<issuer>/.well-known/openid-configuration` |
| `DEFDO_DEV_CLIENT_ID` | OAuth public client id | `defdo-telecom-mobile-dev` |
| `DEFDO_DEV_REDIRECT_URI` | OAuth redirect (App/Universal Link) | `https://login.defdo-telecom.example/mobile/oauth/callback` |
| `DEFDO_DEV_SCOPES` | space-separated scopes | `openid profile offline_access` |
| `DEFDO_BACKEND_BASE_URL` | product BFF base | `https://api.defdo.example` |
| `DEFDO_ENVIRONMENT` | environment label | `dev` |

Android redirect filter host/scheme/path are set via Gradle properties
(`defdo.redirect.scheme/host/path`) → manifest placeholders.

## J. Dev smoke checklist

See `docs/mobile_device_smoke_runbook.md` for the full on-device smoke runbook,
including backend requirements, OAuth registration checklist, App Links /
Universal Links verification, and troubleshooting.

### Android

```sh
# point Gradle at the SDK (this workstation):
#   local.properties -> sdk.dir=/Volumes/data/flutter_apps/android_studio
./gradlew :android:apps:defdo-selfcare:assembleDebug
./gradlew :android:apps:defdo-selfcare:testDebugUnitTest
./gradlew :android:apps:defdo-selfcare:installDebug
adb shell pm clear dev.defdo.selfcare
adb shell monkey -p dev.defdo.selfcare -c android.intent.category.LAUNCHER 1
```

### iOS

```sh
swift build --package-path ios/Apps/DefdoSelfCare
swift test  --package-path ios/Apps/DefdoSelfCare
elixir ios/Apps/DefdoSelfCare/generate_xcodeproj.exs
xcodebuild -project ios/Apps/DefdoSelfCare/DefdoSelfCare.xcodeproj \
  -scheme DefdoSelfCare -destination 'platform=iOS Simulator,name=iPhone 17' build
open ios/Apps/DefdoSelfCare/DefdoSelfCare.xcodeproj
```

## K. Validation results (at implementation time)

- Android `:android:apps:defdo-selfcare:testDebugUnitTest` — **31 passed**.
- Android `:android:apps:defdo-selfcare:assembleDebug` — **APK built**.
- iOS `swift test --package-path ios/Apps/DefdoSelfCare` — **28 passed**.
- iOS `swift build --package-path ios/Apps/DefdoSelfCare` — **build complete**.
- iOS `xcodebuild -project ... -scheme DefdoSelfCare -destination 'platform=iOS Simulator,name=iPhone 17' build` — **BUILD SUCCEEDED**.
- No regressions in existing auth/theme module tests.

## L. Known gaps / next milestone

- Live device auth smoke remains a release gate (see
  `docs/mobile_device_smoke_runbook.md`).
- Theme `mode` follows system light/dark only; no in-app override yet.
- Next recommended milestone: **complete the on-device dev smoke against the
  dev issuer and the bootstrap/theme BFF** (then line linking as a separate
  milestone).
```
