# Defdo Mobile Auth Live Smoke Checklist

Status: pending manual run
Last updated: Milestone 2-F

This checklist must be completed against a configured defdo_auth dev issuer
before auth is considered live-verified for release.

## Pre-flight

- [ ] `DEFDO_DEV_ISSUER` set to a reachable dev issuer URL
- [ ] Dev issuer discovery endpoint is reachable
- [ ] Android APK built and installed
- [ ] Android App Links assetlinks.json deployed (https redirect) OR custom scheme configured
- [ ] iOS harness running in Xcode with scheme env vars set
- [ ] iOS apple-app-site-association deployed (Universal Link) OR custom URL scheme registered
- [ ] Dev issuer has client_id and redirect_uri registered for the harness

## Android Live Smoke

### Discovery

- [ ] Discovery document loaded from `{issuer}/.well-known/openid-configuration`
- [ ] issuer matches expected issuer
- [ ] authorization_endpoint is HTTPS and host matches issuer
- [ ] token_endpoint is HTTPS and host matches issuer
- [ ] revocation_endpoint present (optional, validated if present)
- [ ] code_challenge_methods_supported includes S256 (if field present)

### Authorization

- [ ] Authorization URL built with PKCE S256
- [ ] URL contains response_type=code
- [ ] URL contains code_challenge_method=S256
- [ ] URL contains state and nonce
- [ ] PKCE verifier is NOT in URL
- [ ] Browser opened (Chrome Custom Tabs)

### Callback

- [ ] Callback received via App Link / custom scheme
- [ ] Callback validated: exact redirect URI component match
- [ ] Callback validated: state matches expected
- [ ] Callback validated: code is present
- [ ] Callback validated: no duplicated params
- [ ] Raw callback URL NEVER logged

### Token Exchange

- [ ] Authorization code exchanged at token_endpoint
- [ ] Token response parsed: access_token present
- [ ] Token response parsed: token_type is Bearer or absent
- [ ] Non-Bearer token_type rejected before storing
- [ ] refresh_token present (if offline_access scope)
- [ ] id_token parsed (if present)
- [ ] Tokens stored via AndroidKeystoreSecureStorageAdapter
- [ ] Stored envelope uses versioned JSON format

### Session Management

- [ ] currentSession() returns stored session
- [ ] Refresh succeeds when refresh_token exists
- [ ] Refresh rotates refresh_token if issuer returns new one
- [ ] Refresh keeps existing refresh_token if issuer omits it
- [ ] Refresh with invalid_grant clears local session
- [ ] Logout/revoke clears local session
- [ ] Logout/revoke clears session even if remote revoke fails

### Security

- [ ] No access_token in logs
- [ ] No refresh_token in logs
- [ ] No id_token in logs
- [ ] No auth code in logs
- [ ] No PKCE verifier in logs
- [ ] No raw callback URL in logs
- [ ] No Authorization header in logs
- [ ] No Set-Cookie header in logs

## iOS Live Smoke

### Discovery

- [ ] Discovery document loaded from `{issuer}/.well-known/openid-configuration`
- [ ] issuer matches expected issuer
- [ ] authorization_endpoint is HTTPS and host matches issuer
- [ ] token_endpoint is HTTPS and host matches issuer
- [ ] code_challenge_methods_supported includes S256 (if field present)

### Authorization

- [ ] Authorization URL built with PKCE S256
- [ ] URL contains response_type=code
- [ ] URL contains code_challenge_method=S256
- [ ] URL contains state and nonce
- [ ] PKCE verifier is NOT in URL
- [ ] Browser opened (ASWebAuthenticationSession)

### Callback

- [ ] Callback received via Universal Link / custom scheme
- [ ] Callback validated: exact redirect URI component match
- [ ] Callback validated: state matches expected
- [ ] Callback validated: code is present
- [ ] Callback validated: no duplicated params
- [ ] Raw callback URL NEVER logged

### Token Exchange

- [ ] Authorization code exchanged at token_endpoint
- [ ] Token response parsed: access_token present
- [ ] Token response parsed: token_type is Bearer or absent
- [ ] Non-Bearer token_type rejected before storing
- [ ] Tokens stored via KeychainSecureStorageAdapter
- [ ] Stored envelope uses versioned JSON format

### Session Management

- [ ] currentSession() returns stored session
- [ ] Refresh succeeds when refresh_token exists
- [ ] Refresh with invalid_grant clears local session
- [ ] Logout/revoke clears local session
- [ ] Logout/revoke clears session even if remote revoke fails

### Security

- [ ] No access_token in logs
- [ ] No refresh_token in logs
- [ ] No id_token in logs
- [ ] No auth code in logs
- [ ] No PKCE verifier in logs
- [ ] No raw callback URL in logs
- [ ] No Authorization header in logs
- [ ] No Set-Cookie header in logs

## Sign-off

- [ ] Android live smoke completed: YES / NO
- [ ] iOS live smoke completed: YES / NO
- [ ] Known issues documented: __________________
- [ ] Ready for Theme work to begin in parallel: YES / NO
