# Defdo Mobile Auth Dev Harness Runbook

Status: ready for manual live device verification
Last updated: Milestone 2-F

## Overview

The dev auth harnesses prove the end-to-end auth flow against a configured
defdo_auth dev issuer. They are NOT product apps, NOT CI tests, and NOT intended
for end users.

This runbook covers:

- Android dev harness: `android/apps/dev-auth-harness/`
- iOS dev harness: `ios/Apps/DevAuthHarness/`

## Environment Variables

All required and optional environment variables:

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DEFDO_DEV_ISSUER` | **yes** | (none) | Issuer base URL, e.g. `https://auth.dev.defdo.example` |
| `DEFDO_DEV_DISCOVERY_URL` | no | `$ISSUER/.well-known/openid-configuration` | Override discovery URL |
| `DEFDO_DEV_CLIENT_ID` | no | `dev-client` | OAuth client_id registered with issuer |
| `DEFDO_DEV_REDIRECT_URI` | no | `https://app.defdo.example/oauth/callback` | Must match issuer registration |
| `DEFDO_DEV_SCOPES` | no | `openid profile offline_access` | Space-separated scopes |

The harness shows "Not configured" and disables all buttons when
`DEFDO_DEV_ISSUER` is absent. Normal CI never sets this variable.

---

## Android

### Prerequisites

- Android SDK 34+ installed
- `ANDROID_HOME` environment variable set
- Android device or emulator (API 26+)

### Build

```bash
ANDROID_HOME=/path/to/sdk ./gradlew :android:apps:dev-auth-harness:assembleDebug
```

APK output: `android/apps/dev-auth-harness/build/outputs/apk/debug/`

### Redirect Manifest Placeholders

The App Link intent filter is generated from Gradle properties:

| Property | Placeholder | Default |
|----------|-------------|---------|
| `defdo.redirect.scheme` | `defdoAuthRedirectScheme` | `https` |
| `defdo.redirect.host` | `defdoAuthRedirectHost` | `app.defdo.example` |
| `defdo.redirect.path` | `defdoAuthRedirectPathPrefix` | `/oauth/callback` |

Override at build time:

```bash
./gradlew :android:apps:dev-auth-harness:assembleDebug \
  -Pdefdo.redirect.scheme=custom \
  -Pdefdo.redirect.host=myapp.example.com \
  -Pdefdo.redirect.path=/auth/callback
```

Deploy with environment variables set in the device shell or via `adb shell setprop`.

### App Links (https redirect)

For https redirect URIs, the issuer domain must serve a Digital Asset Links file
at:

```
https://{host}/.well-known/assetlinks.json
```

Example `assetlinks.json`:

```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "dev.defdo.mobile.harness",
    "sha256_cert_fingerprints": ["REPLACE_WITH_DEBUG_KEY_FINGERPRINT"]
  }
}]
```

Get the debug key fingerprint:

```bash
keytool -list -v -keystore ~/.android/debug.keystore \
  -alias androiddebugkey -storepass android | grep SHA256
```

The `autoVerify="true"` attribute in the manifest requires this file to be
valid and reachable. Without it, the browser may show a chooser dialog instead
of directly opening the app.

### Dev-Only Custom Scheme (if allowed by defdo_auth)

If the dev issuer is configured to accept custom scheme redirects:

1. Set `defdo.redirect.scheme` to the custom scheme (e.g. `defdoauth`)
2. Set `defdo.redirect.host` to the callback host (or empty for scheme-only)
3. The Intent filter automatically matches the configured scheme

### Install and Run

```bash
# Install APK
adb install android/apps/dev-auth-harness/build/outputs/apk/debug/dev-auth-harness-debug.apk

# Launch
adb shell am start -n dev.defdo.mobile.harness/.DevAuthHarnessActivity
```

### Manual Steps

1. Launch harness app
2. Verify status shows "configured: https://..."
3. Tap **Start Login** — Chrome Custom Tabs opens authorization URL
4. Complete login in browser with dev issuer credentials
5. Browser redirects to `{redirect_uri}?code=...&state=...`
6. OS routes callback to harness via App Link or custom scheme
7. Verify status shows "authenticated"
8. Tap **Refresh** → verify "refresh succeeded"
9. Tap **Logout** → verify "logged out"
10. Verify session cleared — tap Refresh again, shows "no session"

---

## iOS

### Prerequisites

- Xcode 16+ (for Swift 6.0 tools-version)
- iOS 16+ device or simulator
- DefdoAuthMobile Swift package at `ios/Packages/DefdoAuthMobile`

### Build (macOS Verification)

```bash
swift build --package-path ios/Apps/DevAuthHarness
```

This builds the harness binary for macOS verification. It proves the source
compiles and links against DefdoAuthMobile, but does NOT produce an iOS app
bundle.

### Xcode Setup for iOS Deployment

1. Open `ios/Apps/DevAuthHarness/Package.swift` in Xcode
2. Select the **DevAuthHarness** scheme
3. Edit scheme → Run → Arguments → Environment Variables:
   - Add `DEFDO_DEV_ISSUER` with the dev issuer URL
   - Add any optional variables as needed
4. Under Signing & Capabilities, add the **Associated Domains** capability:
   - `applinks:{redirect_host}` (for Universal Links)
5. Ensure the bundle identifier matches or is configured for the redirect URI

### Universal Links (production recommended)

For https redirect URIs via Universal Links:

1. The redirect host must serve an `apple-app-site-association` file at:
   - `https://{host}/.well-known/apple-app-site-association`
   - OR `https://{host}/apple-app-site-association`

Example `apple-app-site-association`:

```json
{
  "applinks": {
    "apps": [],
    "details": [{
      "appID": "TEAM_ID.dev.defdo.mobile.harness",
      "paths": ["/oauth/callback"]
    }]
  }
}
```

2. Add the **Associated Domains** capability in Xcode with:
   ```
   applinks:{redirect_host}
   ```

### Custom URL Scheme (dev only, if allowed by defdo_auth)

The harness uses `ASWebAuthenticationSession` with a `callbackURLScheme`
derived from the redirect URI scheme (the part before `://`).

For custom scheme redirect URIs like `defdoauth://callback`:

1. The `callbackURLScheme` is automatically set to the scheme portion of
   `DEFDO_DEV_REDIRECT_URI`
2. Register the custom URL scheme in Xcode under Info → URL Types
3. The redirect URI must be registered with the dev issuer

**Do NOT derive a custom callback scheme from an https redirect URI.**
Custom schemes and https redirects are different authentication mechanisms
and must be configured separately on both the client and issuer side.

### Manual Steps

1. Run harness from Xcode on device/simulator
2. Verify status shows "configured: https://..."
3. Tap **Start Login** — ASWebAuthenticationSession opens
4. Complete login in browser
5. Callback returns to app via Universal Link or custom scheme
6. Verify status shows "authenticated"
7. Tap **Refresh** → verify "refresh succeeded"
8. Tap **Logout** → verify "logged out"
9. Tap **Clear Local Session** → verify "session cleared"

---

## Security

The harness never logs sensitive values. Status output uses only:

- `discovery loaded`
- `authorization URL built`
- `browser opened`
- `callback received`
- `callback valid`
- `token exchange succeeded`
- `token stored`
- `refresh succeeded`
- `logout cleared session`

The following are NEVER displayed or logged:

- access_token
- refresh_token
- id_token
- authorization code
- PKCE verifier
- raw callback URL
- Authorization header
- Set-Cookie header

---

## Troubleshooting

| Symptom | Likely Cause |
|---------|-------------|
| "Not configured" | `DEFDO_DEV_ISSUER` env var not set |
| "discovery invalid" | Issuer not reachable or discovery endpoint incorrect |
| Browser opens but callback not received | App Link / Universal Link not configured on issuer domain |
| "login failed" | Token exchange rejected (check issuer logs) |
| "no session" on Refresh | Token not stored or previously cleared |
| APK install fails | Device API level < 26 or debug signing mismatch |
