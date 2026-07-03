# Mobile Device Smoke 3-B Runbook

Status: ready for on-device dev smoke
Target: Defdo SelfCare app shells (Android + iOS)
Scope: Install, launch, OAuth callback, bootstrap, and theme smoke. No
line-linking, no payments, no usage/consumption screens.

## Quick links

- Android app: `android/apps/defdo-selfcare`
- iOS app: `ios/Apps/DefdoSelfCare`
- Architecture: `docs/mobile_app_shell.md`
- Backend contract: `docs/mobile_backend_bootstrap_and_theme.md`

---

## A. Required backend services

Before installing the app, ensure these services are running and reachable from
the device:

| Service | Requirement |
|---------|-------------|
| `defdo_auth` dev issuer | reachable over HTTPS; supports Authorization Code + PKCE for the mobile client |
| `core_graph` BFF | `POST /mobile/bootstrap` and `GET /mobile/theme` implemented and reachable |
| Tenant host | `TenantFromHost` (or equivalent edge resolver) can resolve the tenant used by the BFF |
| `actor_apis` row | a mobile OAuth client is registered with the client_id used by the app |
| Actor metadata | `brand_key`, `app_key`, `platform`, `environment`, `bootstrap_enabled: true`, `theme_enabled: true` |
| Published theme | a `type: :mobile` theme is published for the tenant |
| Test user | a user that can log in through the dev issuer |
| Linked subscriber (optional) | required only for the `ready` state; without one the app shows `needs_line_linking` |

Authority rule (non-negotiable): mobile sends **only** the bearer token to
`/mobile/bootstrap` and `/mobile/theme`. It never sends `brand_code`,
`brand_key`, `app_key`, `theme_code`, or `tenant` as request authority. The
backend derives brand/app/tenant from the token + AccessContext.

---

## B. OAuth client registration checklist

### Android

| Field | Dev value / example |
|-------|---------------------|
| `client_id` | `defdo-telecom-mobile-dev` (or tenant-specific mobile client) |
| `redirect_uri` | `https://login.defdo-telecom.example/mobile/oauth/callback` (production target) |
| `package_name` / app id | `dev.defdo.selfcare` |
| App Links domain | `login.defdo-telecom.example` |
| Custom scheme (dev-only) | `defdo.selfcare.dev://oauth.callback` |
| Debug signing fingerprint | SHA-256 fingerprint of `~/.android/debug.keystore` (see section D.1) |

### iOS

| Field | Dev value / example |
|-------|---------------------|
| `client_id` | `defdo-telecom-mobile-dev` (or tenant-specific mobile client) |
| `redirect_uri` | `https://login.defdo-telecom.example/mobile/oauth/callback` (production target) |
| `bundle_id` | `dev.defdo.selfcare` |
| Universal Links domain | `login.defdo-telecom.example` |
| Custom scheme (dev-only) | `defdo.selfcare.dev` |
| Associated domains | `applinks:login.defdo-telecom.example` |

---

## C. Dev config keys

### Android

Values can be supplied via Gradle properties, environment variables, or left at
the non-secret defaults. They are baked into `BuildConfig` at compile time.

| Gradle property | Environment variable | Default |
|-----------------|----------------------|---------|
| `defdo.dev.issuer` | `DEFDO_DEV_ISSUER` | (empty) |
| `defdo.dev.discoveryUrl` | `DEFDO_DEV_DISCOVERY_URL` | `{issuer}/.well-known/openid-configuration` |
| `defdo.dev.clientId` | `DEFDO_DEV_CLIENT_ID` | `defdo-telecom-mobile-dev` |
| `defdo.dev.redirectUri` | `DEFDO_DEV_REDIRECT_URI` | `https://login.defdo-telecom.example/mobile/oauth/callback` |
| `defdo.dev.scopes` | `DEFDO_DEV_SCOPES` | `openid profile offline_access` |
| `defdo.backendBaseUrl` | `DEFDO_BACKEND_BASE_URL` | `https://api.defdo.example` |
| `defdo.environment` | `DEFDO_ENVIRONMENT` | `dev` |

Redirect manifest placeholders (used for the intent filter):

| Gradle property | Default |
|-----------------|---------|
| `defdo.redirect.scheme` | `https` |
| `defdo.redirect.host` | `login.defdo-telecom.example` |
| `defdo.redirect.path` | `/mobile/oauth/callback` |

Dev signing overrides:

| Gradle property | Environment variable | Default |
|-----------------|----------------------|---------|
| `defdo.dev.storeFile` | `DEFDO_DEV_STORE_FILE` | `~/.android/debug.keystore` |
| `defdo.dev.storePassword` | `DEFDO_DEV_STORE_PASSWORD` | `android` |
| `defdo.dev.keyAlias` | `DEFDO_DEV_KEY_ALIAS` | `androiddebugkey` |
| `defdo.dev.keyPassword` | `DEFDO_DEV_KEY_PASSWORD` | `android` |

### iOS

Values are read from `DefdoSelfCare/Info.plist` at launch, with environment
variables as fallback. Edit the plist before building, or override with the
Xcode scheme's environment variables.

| Info.plist key | Default |
|----------------|---------|
| `DEFDO_DEV_ISSUER` | (empty) |
| `DEFDO_DEV_DISCOVERY_URL` | (empty; derived from issuer) |
| `DEFDO_DEV_CLIENT_ID` | `defdo-telecom-mobile-dev` |
| `DEFDO_DEV_REDIRECT_URI` | `https://login.defdo-telecom.example/mobile/oauth/callback` |
| `DEFDO_DEV_SCOPES` | `openid profile offline_access` |
| `DEFDO_BACKEND_BASE_URL` | `https://api.defdo.example` |
| `DEFDO_ENVIRONMENT` | `dev` |

---

## D. Build and install smoke commands

### D.1 Android

```sh
# 1. Build debug APK
./gradlew :android:apps:defdo-selfcare:assembleDebug

# 2. Run unit tests
./gradlew :android:apps:defdo-selfcare:testDebugUnitTest

# 3. Install on a connected device or running emulator
./gradlew :android:apps:defdo-selfcare:installDebug

# If you prefer adb directly:
adb install -r android/apps/defdo-selfcare/build/outputs/apk/debug/defdo-selfcare-debug.apk

# 4. Clear app data before a fresh smoke run
adb shell pm clear dev.defdo.selfcare

# 5. Launch the app
adb shell monkey -p dev.defdo.selfcare -c android.intent.category.LAUNCHER 1

# 6. Capture non-token logs filtered to the app tag (safe, no secrets)
adb logcat -d -s DefdoSelfCare:D DefdoAuthMobile:D DefdoThemeMobile:D

# 7. Compute the debug signing fingerprint for App Links / assetlinks.json
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
# Copy the SHA256 fingerprint into assetlinks.json.
```

### D.2 iOS

```sh
# 1. Build the SwiftPM package (macOS toolchain, no simulator required)
swift build --package-path ios/Apps/DefdoSelfCare

# 2. Run unit tests
swift test --package-path ios/Apps/DefdoSelfCare

# 3. Generate/regenerate the Xcode project from Elixir
elixir ios/Apps/DefdoSelfCare/generate_xcodeproj.exs

# 4. Build for iOS simulator with xcodebuild
xcodebuild -project ios/Apps/DefdoSelfCare/DefdoSelfCare.xcodeproj \
  -scheme DefdoSelfCare \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  build

# 5. Run tests through Xcode (if configured)
xcodebuild -project ios/Apps/DefdoSelfCare/DefdoSelfCare.xcodeproj \
  -scheme DefdoSelfCare \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  test

# 6. Open in Xcode for device signing / run
open ios/Apps/DefdoSelfCare/DefdoSelfCare.xcodeproj
```

Before running on a real device, set a Development Team in Xcode and update the
bundle identifier/associated domains if your provisioning profile requires it.

---

## E. App Links / Universal Links verification

### Android assetlinks.json

Host this file at `https://login.defdo-telecom.example/.well-known/assetlinks.json`:

```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "dev.defdo.selfcare",
    "sha256_cert_fingerprints": [
      "AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99"
    ]
  }
}]
```

Replace the fingerprint with the SHA-256 of the signing certificate used to
build the APK.

Validation:

```sh
adb shell pm verify-app-links --re-verify dev.defdo.selfcare
adb shell pm get-app-links dev.defdo.selfcare
```

### iOS apple-app-site-association

Host this file at `https://login.defdo-telecom.example/.well-known/apple-app-site-association`:

```json
{
  "applinks": {
    "apps": [],
    "details": [{
      "appID": "TEAM_ID.dev.defdo.selfcare",
      "paths": ["/mobile/oauth/callback"]
    }]
  }
}
```

Replace `TEAM_ID` with your Apple Developer Team ID. The entitlements file in
the project declares `applinks:login.defdo-telecom.example`.

Validation:

```sh
# On a device, trigger a re-download of the association after installing the app
# Settings > Developer > Universal Links > Diagnostics (or use swcd logs)
```

### Dev-only custom schemes

Both platforms register a custom scheme (`defdo.selfcare.dev`) for local smoke
when App Links / Universal Links are not yet deployed. This is **dev-only**.
Production must rely on verified deep links.

---

## F. Expected smoke outcomes

### F.1 Signed-out cold start

- App launches with the embedded fallback theme.
- No access token is present in secure storage.
- Login CTA is visible.
- `/mobile/bootstrap` and `/mobile/theme` are **not** called.

### F.2 Login

- Tapping login opens the system browser (Chrome Custom Tabs on Android,
  ASWebAuthenticationSession on iOS) pointed at `defdo_auth`.
- After successful authentication, the browser redirects to the configured
  redirect URI.
- The app receives the callback, validates state/code, exchanges the code, and
  stores tokens in platform secure storage (Keystore / Keychain).
- Tokens are **never** logged.

### F.3 Bootstrap

- `POST /mobile/bootstrap` is called with `Authorization: Bearer <token>` only.
- Request body does **not** contain `brand_code`, `brand_key`, `app_key`,
  `theme_code`, or `tenant`.
- 200 `needs_line_linking` → placeholder screen.
- 200 `ready` → home placeholder with tenant label.
- 401 → session cleared, app returns to signed-out.
- 403 → safe error screen, not retryable.

### F.4 Theme

- `GET /mobile/theme` is called with `Authorization` and `If-None-Match` when a
cached ETag exists.
- 200 → runtime theme applied and ETag cached.
- 304 → cached theme used.
- 403/404 → fallback / last-known-good theme kept; dev-only diagnostic attached.
- Theme endpoint unavailable → no crash, app stays on fallback theme.

---

## G. Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| `redirect_uri mismatch` error in browser | OAuth client registration does not match `DEFDO_DEV_REDIRECT_URI` / `DEFDO_DEV_CLIENT_ID` | Update the issuer's client registration or the app's config |
| Callback opens browser instead of app | App Links / Universal Links not verified | Deploy `assetlinks.json` / `apple-app-site-association`; for dev only, switch to the custom scheme |
| Bootstrap returns 401 immediately | Token expired or invalid clock skew | Check token expiry and device clock; enable NTP |
| Bootstrap returns 403 | Tenant/app does not allow this subject, or actor metadata missing | Verify actor `client_id`, `brand_key`, `app_key`, and that the user is allowed |
| Bootstrap returns 500 / malformed | `bootstrap_enabled` false or actor metadata missing | Set `bootstrap_enabled: true` and populate actor metadata |
| Theme never updates | `theme_enabled` false or no published mobile theme | Set `theme_enabled: true` and publish a `type: :mobile` theme |
| App crashes on launch with bad theme | Invalid cached theme | Clear app data / uninstall; cache validation will discard it on next boot |
| App stuck on fallback theme | Theme endpoint unreachable or returns 403/404 | Check network and backend theme endpoint |
| iOS Keychain error | Missing or mismatched keychain access group | Verify entitlements `keychain-access-groups` matches provisioning profile |
| Android install fails | APK signed with wrong certificate / App Links verification pending | Use debug keystore; for production, ensure `assetlinks.json` fingerprint matches signing cert |
| Tenant host mismatch | Device resolves a different host than the BFF expects | Update DNS / hosts and confirm `TenantFromHost` configuration |
| Clock skew | Device time differs from server | Enable automatic date/time on the device |

---

## H. Sign-off

- [ ] Android APK builds (`:android:apps:defdo-selfcare:assembleDebug`)
- [ ] Android unit tests pass (`:android:apps:defdo-selfcare:testDebugUnitTest`)
- [ ] Android App Links intent filter present in manifest
- [ ] Android dev signing config present and non-production
- [ ] iOS SwiftPM build passes (`swift build --package-path ios/Apps/DefdoSelfCare`)
- [ ] iOS SwiftPM tests pass (`swift test --package-path ios/Apps/DefdoSelfCare`)
- [ ] iOS `.xcodeproj` generated and present
- [ ] iOS Info.plist has bundle id, display name, custom URL scheme
- [ ] iOS entitlements declare associated domains and keychain access group
- [ ] On-device OAuth smoke completed: Android _____________ iOS _____________
- [ ] Known issues documented: ________________________________
