# Mobile Bootstrap and Runtime Theme Integration

Status: proposed
Target: defdo_my_mvno / Mobile BFF + defdo_mobile clients
Last updated: 2026-06-15 (post Milestone 2-F)

## A. Executive Summary

defdo_mobile needs a backend endpoint for **mobile bootstrap** (session
establishment, tenant resolution, profile lookup, feature flags) and a
**runtime theme delivery** contract that works for native mobile clients.

This document designs both, grounded in the current backend codebases at:

- `defdo_core_graph` (`/Volumes/data/defdo_projects/core_graph`)
- `defdo_my_mvno` (`/Volumes/data/defdo_projects/defdo_my_mvno`)
- `defdo_tenant_plug` (`/Volumes/data/defdo_projects/defdo_tenant_plug`)
- `defdo_auth_client` (`/Volumes/data/defdo_projects/defdo_auth_client`)

Key principle: **defdo_auth owns identity. defdo_my_mvno owns product state.**
Mobile clients never talk to defdo_auth directly after token exchange.

---

## B. Current Backend Findings

### B.1 Tenant Model

`defdo_tenant_plug` resolves tenants via a chain of Plug resolvers:

| Resolver | Source | Mechanism |
|----------|--------|-----------|
| `:host` | `conn.host` | Domain lookup via `Defdo.Tenant.get_profile_by(%{"via_domain" => host})` |
| `:subdomain` | subdomain extract | Strips root domain, uses subdomain as tenant_id |
| `:header` | `X-Tenant-Id` header | Value used directly as tenant_id |
| `:session` | Plug session | Reads `"tenant_id"` key |
| `:path` | Path segment | Index-based extraction from URL |
| `:static` | Fixed | Always resolves to configured tenant_id |

**Critical finding:** Tenant is a **process-level context** established at the
edge (HTTP request, WebSocket connect) via `Defdo.Tenant.inject_tenant/1`.
All downstream queries (Repo, Ash, everything) read tenant from process
dictionary automatically. This means:

- Mobile clients must NOT assert a tenant directly.
- Tenant must be resolved server-side from a trusted combination of
  verified token claims, OAuth client_id, and backend registry.

`defdo_core_graph` has a `TenantProfile` schema with fields: `name`, `region`,
`environment`, `code`, `logo`, `theme`, `tier`, `domain`, `custom_domain`,
`is_active`, `is_deleted`. This is the current tenant resource.

### B.2 Auth Client Model

`defdo_auth_client` provides **opaque token introspection** via the OAuth2
`/oauth/introspect` endpoint. It does NOT perform cryptographic JWT verification.

Token validation flow:
1. Extract `Bearer <token>` from `Authorization` header
2. POST `{token, client_id, client_secret}` to `/oauth/introspect`
3. Server returns `{"active": bool, "scope": "...", "sub": "...", "exp": int, ...}`
4. Client trusts the server response

`Defdo.AuthPlug` (the Plug middleware) extracts the Bearer token, introspects
it, validates scopes, and assigns `:current_user` (`%{id: sub}`) and
`:token_info` to `conn.assigns`.

The `Defdo.Auth.TokenManager` wraps this with **cache-based TTL management**,
auto-refresh near expiry, and invalidation on revocation.

**The `sub` claim** from introspection is the primary identity link. It is
extracted in:
- `Defdo.AuthPlug` line 288: `Map.get(token_info, "sub")` → `conn.assigns.current_user`
- `PhoenixHelpers` line 413: `Map.get(token_info, "sub")` → user map `:id`

### B.3 Existing Identity Linking

`defdo_core_graph` has an `Identity` schema at
`lib/defdo_core_graph/accounts/schema/identity.ex` that links an auth
subject to internal records:

```
Identity:
  belongs_to :tenant, Profile
  belongs_to :user, User (if present)
  field :provider, :string          # "defdo_auth"
  field :provider_uid, :string      # auth subject (sub claim)
  field :email, :string
  field :metadata, :map
  field :last_login_at, :utc_datetime
```

The OAuth controller (`defdo_core_graph_web/controllers/oauth_controller.ex`)
calls `Accounts.find_or_create_identity/2` after successful OAuth callback.

**This is the existing identity link.** It maps `provider + provider_uid` to
a tenant-scoped identity record. Mobile bootstrap should use the same
mechanism.

### B.4 SelfCare / Subscriber Model

`defdo_core_graph` has a `SelfCare` context with:

- `Group` — groups subscribers together (family plans, etc.)
- `Subscriber` — the self-care profile with fields: `theme_code`, `theme_mode`,
  `msisdn`, status, and associations to lines, transactions, managed services

The subscriber IS the self-care profile. Line ownership is managed through
subscriber-line associations via Altan API integration.

### B.5 Theme Infrastructure

Theme is served through multiple mechanisms:

1. **GraphQL** — `theme_queries` provide `get_config/3` by code via
   `Defdo.Theme.get_config_by/1`
2. **Theme Hub** — embedded in the admin dashboard at
   `scope "/dashboard/admin/platform/themes"` with LiveView management UI
3. **CSS serving** — `scope "/api/themes"` for theme CSS files
4. **PubSub adapter** — `ThemeHubPubSubAdapter` for subscription/invalidation
5. **Theme resource (Ash)** — `MyMvno.Resource.Theme` at table `defdo_themes`
   with attributes: `type` (web/desktop/mobile/custom), `mode` (light/dark),
   `colors`, `fonts`, `tags`, `metadata`

**Key finding:** Theme `type` already supports `:mobile`. Subscribers already
have `theme_code` and `theme_mode` fields. The infrastructure exists but
needs a stable mobile-facing HTTP contract.

### B.6 Mobile Readiness

The codebase already has mobile-aware design:

| Feature | Location | Status |
|---------|----------|--------|
| Actor `app_type: :mobile` | `my_mvno/resource/actor.ex` | **EXISTS** |
| Theme `type: :mobile` | `my_mvno/resource/theme.ex` | **EXISTS** |
| `oauth2_code_pkce` required for mobile actors | `actor.ex` validation | **EXISTS** |
| `consumption_with_packages` (mobile-optimized query) | GraphQL subscriber type | **EXISTS** |
| `check_line_status_public` (mobile pre-registration) | GraphQL subscriber type | **EXISTS** |
| Mobile bootstrap endpoint | — | **MISSING** |
| Mobile runtime theme HTTP contract | — | **MISSING** |

---

## C. Proposed Mobile Bootstrap Flow

```
MOBILE CLIENT                         defdo_auth (IDP)           MOBILE BFF
     │                                      │                       │
     │  1. Load embedded brand manifest     │                       │
     │  2. Load embedded fallback theme     │                       │
     │  3. Load cached last-known-good theme│                       │
     │                                      │                       │
     │  4. GET discovery                    │                       │
     │───────/oauth/authorize?PKCE──────────>                       │
     │                                      │                       │
     │  5. Hosted login/signup              │                       │
     │<──────redirect_uri?code=&state=──────│                       │
     │                                      │                       │
     │  6. Exchange code for tokens         │                       │
     │───────POST /oauth/token──────────────>                       │
     │<──────{access_token, refresh_token}──│                       │
     │                                      │                       │
     │  7. Store tokens securely            │                       │
     │     (Keystore / Keychain)            │                       │
     │                                      │                       │
     │  8. POST /mobile/bootstrap           │                       │
     │     Authorization: Bearer <token>    │                       │
     │     X-Defdo-Brand-Key: ...           │                       │
     │     X-Defdo-App-Key: ...             │                       │
     │──────────────────────────────────────────────────────────────>│
     │                                      │   9. Introspect token  │
     │                                      │<──POST /oauth/introspect
     │                                      │───{active, sub, scope}->│
     │                                      │                       │
     │                                      │   10. Resolve tenant   │
     │                                      │   from client_id +     │
     │                                      │   brand/app manifest   │
     │                                      │                       │
     │                                      │   11. Lookup identity  │
     │                                      │   by sub + provider    │
     │                                      │                       │
     │                                      │   12. Find or create   │
     │                                      │   self-care profile    │
     │                                      │                       │
     │<──{bootstrap payload}────────────────────────────────────────│
     │                                      │                       │
     │  13. Resolve feature flags           │                       │
     │  14. Fetch runtime theme if stale    │                       │
     │  15. Validate theme                  │                       │
     │  16. Apply theme                     │                       │
     │  17. Navigate to app shell state     │                       │
```

### Step Details

1-3. **Pre-auth boot**: Brand manifest + fallback theme + cached theme load
before any network call. App renders with last-known-good state.

4-7. **Auth flow**: Standard Authorization Code + PKCE against defdo_auth.
defdo_auth owns the entire hosted login/signup experience.

8. **Bootstrap call**: Mobile sends the access token + brand/app context to
the Mobile BFF. Token is never sent to any other service.

9. **Token verification**: BFF introspects the token via defdo_auth to get
`sub`, `active`, `scope`, `client_id`, `aud`.

10. **Tenant resolution**: BFF resolves tenant from `client_id` (extracted
from token introspection) mapped to brand/app manifest. Mobile-provided
`X-Defdo-Brand-Key` is validated against the token's client_id, never trusted
blindly.

11-12. **Identity and profile**: BFF looks up or creates the identity link
(`provider + sub`) and the self-care profile.

13-17. **Post-bootstrap**: Feature flags, theme, navigation state.

---

## D. User/Identity/Profile Ownership Model

### Where records are created

| Record Type | Owned By | Created When |
|-------------|----------|-------------|
| OAuth identity (subject, credentials) | **defdo_auth** | During hosted login/signup |
| OAuth token + claims | **defdo_auth** | Token issuance |
| Identity link (provider + sub → internal) | **defdo_my_mvno / Mobile BFF** | First bootstrap after new subject |
| Self-care profile (customer) | **defdo_my_mvno / Mobile BFF** | First bootstrap (if auto-create enabled) or manual profile completion |
| Subscriber/line ownership | **defdo_my_mvno** | Line activation via Altan API |
| Tenant/account records | **defdo_my_mvno** | Tenant provisioning (admin) |

### Separation Rules

1. **defdo_auth must not create MVNO product records directly.**
2. Bootstrap is the only path from auth identity to MVNO profile.
3. Line ownership is NEVER created from mobile input alone — it requires
   backend verification (e.g., Altan activation, MSISDN verification).
4. Public signup must be explicitly enabled per tenant/app.

---

## E. Bootstrap Endpoint Contract

### Request

```
POST /mobile/bootstrap
```

**Headers:**

| Header | Required | Description |
|--------|----------|-------------|
| `Authorization` | yes | `Bearer <access_token>` |
| `X-Defdo-Brand-Key` | yes | Brand identifier (validated against token) |
| `X-Defdo-App-Key` | yes | App identifier within brand |
| `X-Defdo-Platform` | yes | `android` or `ios` |
| `X-Defdo-App-Version` | yes | Semver string |
| `X-Defdo-Device-Id` | no | Only if privacy-approved by user |

### Response Statuses

| Status | HTTP | Meaning |
|--------|------|---------|
| `ready` | 200 | Full access, profile exists |
| `needs_profile_completion` | 200 | Identity linked, profile incomplete |
| `needs_line_linking` | 200 | Profile exists, no active line |
| `needs_terms_acceptance` | 200 | Terms of service not accepted |
| `suspended` | 403 | Account suspended |
| `blocked` | 403 | Account blocked |
| `no_access` | 403 | Tenant/app does not allow this subject |

### Response Payload

```json
{
  "status": "ready",
  "tenant": {
    "id": "uuid",
    "code": "defdo-telecom",
    "name": "Defdo Telecom",
    "region": "mx",
    "environment": "production"
  },
  "brand": {
    "key": "defdo-telecom",
    "display_name": "Defdo Telecom",
    "logo_url": "https://cdn.defdo.example/brands/defdo-telecom/logo.png"
  },
  "app": {
    "key": "defdo-telecom-mobile",
    "platform": "android",
    "min_version": "1.0.0",
    "latest_version": "1.2.0",
    "update_url": "https://play.google.com/..."
  },
  "subject": {
    "id": "auth-sub-hash",
    "provider": "defdo_auth"
  },
  "customer": {
    "id": "uuid",
    "status": "active",
    "display_name": null,
    "email_hash": "sha256-hash",
    "needs_profile_completion": false,
    "terms_accepted_at": "2025-01-01T00:00:00Z"
  },
  "lines": [
    {
      "id": "uuid",
      "msisdn_hash": "sha256-hash",
      "status": "active",
      "type": "prepaid",
      "balance": null
    }
  ],
  "required_actions": [],
  "feature_flags": {
    "dark_mode": true,
    "selfcare_portability": false,
    "push_notifications": true
  },
  "theme": {
    "version": 42,
    "stale": false,
    "expires_at": "2025-01-02T00:00:00Z"
  },
  "support": {
    "chat_enabled": true,
    "phone_number_hash": "sha256-hash",
    "hours": "24/7"
  },
  "api_compatibility": "2025-01"
}
```

### Rules

- Do NOT return `access_token`, `refresh_token`, `id_token`, or raw claims.
- Hash `email`, `msisdn`, `phone_number` unless the user has explicitly
  approved their display and the app context requires it.
- `subject.id` should be a derived non-reversible identifier, not the raw
  `sub` claim.

---

## F. Tenant Resolution and Security Model

### Current resolution (browser-side)

defdo_core_graph resolves tenants via `conn.host` → `Defdo.Tenant.get_profile_by(%{"via_domain" => host})`. The tenant is then injected into the process dictionary via `Defdo.Tenant.inject_tenant/1`.

### Proposed resolution (mobile-side)

Mobile clients do not have a domain-based request like browsers. Tenant MUST be
resolved from:

1. **Verified access token** — the token introspection response contains
   `client_id` and `aud` (audience). These are signed by defdo_auth and
   cannot be forged by the mobile client.

2. **OAuth client_id → brand/app mapping** — a registry table maps
   `client_id` to `brand_key` + `app_key`. This mapping is maintained by
   the backend, NOT sent by the mobile client.

3. **Brand/app manifest validation** — the mobile client sends
   `X-Defdo-Brand-Key` and `X-Defdo-App-Key` for informational purposes.
   These are VALIDATED against the registry but NEVER trusted as the sole
   source of truth.

4. **Backend tenant registry** — the brand → tenant mapping is stored in
   the `TenantProfile` or a brand_registry table. The tenant is resolved
   from this mapping, not from any mobile-provided value.

### The backend must NOT trust only mobile-provided tenant_key

Mobile clients may send `X-Defdo-Brand-Key`. The backend MUST:

1. Introspect the Bearer token to get `client_id`.
2. Look up `client_id` in the brand/app registry.
3. Verify `brand_key` from registry matches `X-Defdo-Brand-Key`.
4. Resolve tenant from the registry's brand → tenant mapping.
5. If `X-Defdo-Brand-Key` does not match the registry, return 403.

### Actor-Based Authorization

defdo_core_graph already has an `Actor` resource with `app_type: :mobile` and
auth method validation requiring `oauth2_code_pkce` for mobile. This should
be extended to:

- Validate that bootstrap calls use a token issued to the correct `client_id`.
- Scope mobile-specific features to `app_type: :mobile` actors.
- Enforce `allowed_scopes` and `required_scopes` on bootstrap and subsequent
  mobile API calls.

---

## G. Identity Linking Model

### Existing schema (defdo_core_graph)

```elixir
# lib/defdo_core_graph/accounts/schema/identity.ex
schema "identities" do
  belongs_to :tenant, Profile
  field :provider, :string          # "defdo_auth"
  field :provider_uid, :string      # auth subject (sub claim)
  field :email, :string
  field :metadata, :map
  field :last_login_at, :utc_datetime
end
```

### Proposed extension: CustomerIdentityLink

The existing `Identity` schema already maps `provider + provider_uid` to a
tenant. For mobile bootstrap, this is sufficient as the identity link.

What is MISSING is the link from identity → self-care profile:

```elixir
# Proposed: add to Identity schema or create a separate link
schema "identities" do
  # existing fields...

  # NEW: link to self-care profile
  belongs_to :subscriber, Defdo.CoreGraph.SelfCare.Subscriber
end
```

Or alternatively, add to the Subscriber:

```elixir
schema "subscribers" do
  # existing fields...

  # link back to identity
  field :auth_subject, :string      # opaque hash of sub
  field :auth_provider, :string     # "defdo_auth"
end
```

**Recommendation:** Add `auth_subject` + `auth_provider` to the Subscriber
schema rather than modifying Identity. This keeps the Identity schema as a
lower-level auth concern and the Subscriber as the product-level profile.

### Uniqueness and Idempotency

```
unique_index :subscribers, [:tenant_id, :auth_provider, :auth_subject]
```

- Bootstrap is idempotent: calling it twice with the same token returns the
  same profile.
- First bootstrap for a new subject auto-creates the subscriber if the
  tenant/app allows public signup.
- Safe retry: no duplicate profiles created.

---

## H. First-Login Scenarios

### Recommended Default

| Tenant setting | Behavior |
|----------------|----------|
| `public_signup: true` | Auto-create minimal self-care profile on first bootstrap |
| `public_signup: false` | Return `needs_line_linking` or `no_access` |
| `auto_link_by_msisdn: true` | If token claims include `phone_number`, attempt MSISDN match |

### Scenario Matrix

| Subject state | Tenant setting | Result |
|---------------|---------------|--------|
| New subject, public signup allowed | `public_signup: true` | Auto-create empty profile → `ready` with `needs_profile_completion` |
| New subject, public signup blocked | `public_signup: false` | `no_access` |
| Known subject, profile exists | any | `ready` |
| Known subject, profile suspended | any | `suspended` |
| Known subject, terms not accepted | any | `needs_terms_acceptance` |
| Known subject, no active line | `require_line: true` | `needs_line_linking` |

### Rules

- NEVER create subscriber/line ownership solely from untrusted mobile input.
- Profile auto-creation produces a minimal record (no MSISDN, no line).
- Line linking requires backend verification (Altan activation, SMS OTP).
- Email/phone from token claims may be used to pre-fill the profile but
  must NOT be trusted as verified without additional confirmation.

---

## I. Runtime Theme Architecture

### Mobile Theme Module Design

```
DefdoThemeClient (Android / iOS)
  │
  ├── ThemeTransport (interface / protocol)
  │     ├── GraphQLThemeTransport    (current adapter)
  │     └── RestThemeTransport       (future adapter)
  │
  ├── ThemeTokenValidator
  │
  ├── ThemeCache (in-memory + disk)
  │
  ├── LastKnownGoodThemeStore
  │
  └── ThemeAdapter (platform-specific rendering)
        ├── ComposeThemeAdapter      (Android)
        └── SwiftUIThemeAdapter      (iOS)
```

### Theme Sources

| Source | When Used | Fallback |
|--------|-----------|----------|
| Embedded fallback theme | First boot, offline, all network fails | Never expires |
| Cached last-known-good theme | Boot with valid cache | Falls back to embedded |
| Runtime theme from server | Online, version changed or cache expired | Falls back to cached |

### Current GraphQL Usage

defdo_core_graph currently serves themes through GraphQL queries. The GraphQL
schema has `theme_queries` with `get_config/3` by code. This is an existing
implementation that must be wrapped behind `ThemeTransport`.

---

## J. Theme Transport Decision

### Recommendation: HTTP REST with ETag as primary; GraphQL behind transport as current implementation

**Rationale:**

1. **Theme updates are not latency-critical.** A stale theme (hours old) is
   perfectly acceptable. Mobile does not need real-time theme updates.

2. **Mobile needs offline-safe boot.** HTTP caching with ETag/If-None-Match
   and Cache-Control headers is well-supported by both platforms' HTTP
   clients (OkHttp on Android, URLSession on iOS). GraphQL POST requests
   are not cacheable by standard HTTP intermediaries.

3. **HTTP caching is simpler and robust.** A GET request with `If-None-Match`
   returns 304 when the theme hasn't changed. This eliminates the need for
   custom cache invalidation logic in the mobile client.

4. **WebSocket increases complexity.** Channels/Phoenix Sockets add lifecycle,
   battery, reconnect, and auth complexity. For theme invalidation, a simple
   push notification or silent background fetch is sufficient.

5. **Last-known-good cache is required anyway.** Even with WebSocket, the app
   must boot with a cached theme before any network call completes.

### Proposed Stable HTTP Contract

```
GET /mobile/theme
```

**Query parameters:**

| Parameter | Required | Description |
|-----------|----------|-------------|
| `brand_key` | yes | Brand identifier |
| `app_key` | yes | App identifier |
| `platform` | yes | `android` or `ios` |
| `mode` | no | `light`, `dark`, or `system` (default: `system`) |
| `version` | no | Last known theme version (for 304) |

**Headers:**

| Header | Required | Description |
|--------|----------|-------------|
| `If-None-Match` | no | ETag from last response |
| `Authorization` | no | `Bearer <token>` if theme requires auth |

**Response (200):**

```json
{
  "version": 42,
  "schema_version": "2025-01",
  "mode": "light",
  "brand_key": "defdo-telecom",
  "app_key": "defdo-telecom-mobile",
  "platform": "android",
  "tokens": {
    "colors": {
      "primary": "#FF6600",
      "surface": "#FFFFFF",
      "on_surface": "#1A1A1A",
      "background": "#FAFAFA",
      "error": "#DC3545",
      "text_primary": "#1A1A1A",
      "text_secondary": "#666666",
      "border": "#E0E0E0",
      "divider": "#F0F0F0",
      "status_bar": "#E65C00",
      "navigation_bar": "#FFFFFF"
    },
    "typography": {
      "heading_font": "Inter",
      "body_font": "Inter",
      "mono_font": "JetBrains Mono",
      "scale_factor": 1.0
    },
    "spacing": {
      "unit": 8,
      "compact": 4,
      "comfortable": 16,
      "relaxed": 24
    },
    "shape": {
      "corner_radius_small": 4,
      "corner_radius_medium": 8,
      "corner_radius_large": 16,
      "button_style": "rounded"
    },
    "icons": {
      "style": "outlined",
      "size": 24
    }
  },
  "contrast_metadata": {
    "minimum_ratio": 4.5,
    "enhanced_ratio": 7.0
  },
  "expires_at": "2025-01-02T00:00:00Z"
}
```

**Response (304 Not Modified):** Empty body. Client uses cached theme.

### Theme transport bridge architecture

```
Mobile Client                    Mobile BFF                  defdo_theme_hub
     │                               │                              │
     │  GET /mobile/theme            │                              │
     │  If-None-Match: "v41"        │                              │
     │───────────────────────────────>                              │
     │                               │  Check theme version         │
     │                               │  (cached or query)           │
     │                               │──────────────────────────────>│
     │                               │<─────current theme version────│
     │                               │                              │
     │  (if version changed)         │                              │
     │                               │  Build mobile-specific       │
     │                               │  semantic token payload      │
     │                               │  from full theme             │
     │                               │                              │
     │<──200 {version:42, tokens}────│                              │
     │                               │                              │
     │  (if version unchanged)       │                              │
     │<──304 Not Modified────────────│                              │
```

### GraphQL role

GraphQL remains the current implementation behind `ThemeTransport`. A
`GraphQLThemeTransport` adapter calls the existing GraphQL query and
transforms the response to the stable mobile contract. This allows:

1. Immediate mobile theme delivery without backend changes.
2. Gradual migration to a dedicated REST endpoint.
3. Mobile clients never coupling to the GraphQL schema directly.

### WebSocket role

WebSocket / Phoenix Channels may be added later ONLY for **invalidation
events** (`theme_version_changed`). The payload would be: `{version: 43}`.
The mobile client then does a standard HTTP pull for the new theme. The
WebSocket never delivers the theme payload itself.

---

## K. Mobile Boot Order

```
1.  Load embedded brand manifest          (from app bundle)
2.  Load embedded fallback theme          (from app bundle)
3.  Load cached last-known-good theme     (from disk cache)
4.  Apply last-known-good or fallback theme
5.  Render app shell (skeleton UI with theme applied)
    │
6.  Initialize Auth module                (DefdoAuthMobileClient)
7.  If session exists, refresh token      (silent, non-blocking)
    │
8.  Fetch bootstrap                       (POST /mobile/bootstrap)
    │   ├── 200: continue
    │   ├── 401: clear session, show login
    │   └── 403: show blocked / no_access
    │
9.  Resolve feature flags
10. Resolve theme version hint             (from bootstrap payload)
11. If theme stale, fetch runtime theme    (GET /mobile/theme)
12. Validate theme                         (ThemeTokenValidator)
13. Apply theme                            (ComposeThemeAdapter / SwiftUIThemeAdapter)
14. Navigate to app shell state:
    ├── logged_out
    ├── needs_profile_completion
    ├── needs_line_linking
    ├── needs_terms_acceptance
    ├── ready
    └── blocked
```

### Timing Constraints

- Steps 1-5 complete within 500ms (all local).
- Step 6-7 complete within 2s (local or cached token refresh).
- Step 8 completes within 3s (network).
- Steps 9-13 complete within 500ms (local, after payload).
- Total time-to-interactive: ≤ 3.5s on cold boot, ≤ 1s on warm boot.

---

## L. Security and Privacy

### Data Sanitization

| Value | Rule |
|-------|------|
| access_token | Never logged, never stored outside Keystore/Keychain |
| refresh_token | Never logged, never stored outside Keystore/Keychain |
| id_token | Never logged, never stored in plaintext |
| Authorization header | Redacted in all logs via AuthRedactor |
| auth code | Never logged, consumed immediately |
| PKCE verifier | Never logged, held in memory only during auth flow |
| raw callback URL | Redacted before any log output |
| claims (sub, email, phone) | Hashed in logs; raw values only in memory during active session |
| MSISDN | Hashed unless user explicitly approved display |
| device_id | Only sent if user approved privacy policy |

### Backend Rules

1. **Bootstrap endpoint must be idempotent.** Repeated calls with the same
   token return the same profile state.
2. **Account/profile creation must be replay-safe.** Duplicate bootstrap
   calls must not create duplicate profiles.
3. **Tenant must not be trusted from mobile headers alone.** Always verify
   against token introspection + brand/app registry.
4. **Theme endpoint must not leak tenant-private data if unauthenticated.**
   If theme is served anonymously, validate that the requested brand_key
   allows public theme access.
5. **If theme endpoint is authenticated**, define fallback behavior: use
   last-known-good theme or embedded fallback theme. Never block app boot
   on a failed theme fetch.

---

## M. Risks and Open Questions

| # | Risk / Question | Impact | Mitigation |
|---|-----------------|--------|------------|
| 1 | `Identity` schema has no explicit link to `Subscriber` | Bootstrap cannot find profile from subject | Add `auth_subject` + `auth_provider` to Subscriber |
| 2 | No `brand_key` → `tenant` registry exists | Mobile tenant resolution depends on this | Create mapping table or extend TenantProfile |
| 3 | Theme served through GraphQL today, not REST | Mobile must use GraphQLThemeTransport initially | Accept as temporary; migrate to REST endpoint |
| 4 | No mobile-specific actor/client registration | Cannot validate mobile-specific scopes | Use existing Actor resource with `app_type: :mobile` |
| 5 | `DEFDO_DEV_ISSUER` config shape vs. production bootstrap | Production needs different env var architecture | Define production config through app bundle, not env vars |
| 6 | Live device auth verification not yet completed | Real-world auth flow untested | Complete smoke checklist before production release |
| 7 | Custom scheme vs Universal Link / App Link decision | Platform redirect mechanism not finalized | Document both; configure per tenant/app |

---

## N. Suggested Milestones

| Milestone | Description | Dependencies |
|-----------|-------------|-------------|
| **M-Theme-1** | Add `auth_subject` + `auth_provider` to Subscriber schema; create identity-to-profile link | Schema migration |
| **M-Theme-2** | Implement `POST /mobile/bootstrap` in Mobile BFF (defdo_my_mvno) | Token introspection, identity link, tenant registry |
| **M-Theme-3** | Implement `GET /mobile/theme` with ETag caching in Mobile BFF | Theme Hub integration |
| **M-Theme-4** | Mobile `ThemeClient` + `ThemeTransport` + `GraphQLThemeTransport` + `RestThemeTransport` | Mobile auth module complete |
| **M-Theme-5** | Mobile `ThemeTokenValidator` + `ThemeCache` + `LastKnownGoodThemeStore` | Theme transport |
| **M-Theme-6** | Mobile `ComposeThemeAdapter` (Android) + `SwiftUIThemeAdapter` (iOS) | Platform theme tokens |
| **M-Theme-7** | End-to-end mobile boot with real bootstrap + theme delivery | All previous |

---

## O. References

- `defdo_tenant_plug` — `/Volumes/data/defdo_projects/defdo_tenant_plug`
  - 27 source files, process-context tenant injection, chain-of-responsibility resolution
- `defdo_auth_client` — `/Volumes/data/defdo_projects/defdo_auth_client`
  - Opaque token introspection, caching via Cachex, `Defdo.AuthPlug` middleware
- `defdo_core_graph` — `/Volumes/data/defdo_projects/core_graph`
  - `Identity` schema, `Subscriber` schema, `Actor` (with `app_type: :mobile`), `Theme` (with `type: :mobile`), GraphQL API, OAuth controller
- `defdo_my_mvno` — `/Volumes/data/defdo_projects/defdo_my_mvno`
  - MVNO package containing SelfCare domain, subscriber management, line ownership
