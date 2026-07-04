# Defdo SelfCare — Requirements & Slice Plan

Status: draft accepted 2026-07-04. Owner: solo dev (paridin).
Scope: Android (`android/apps/defdo-selfcare`) + iOS (`ios/Apps/DefdoSelfCare`).
API owner: **defdo_my_mvno** owns the entire `/mobile/*` BFF from day one —
the apps talk to a single host and the contract never migrates. **core_graph**
stays a data source only: consumption/packages live there for practicality and
defdo_my_mvno reads them server-to-server (M2M client_credentials against
defdo_auth). Later that data may also sync into defdo_my_mvno without any app
change.

## Product goals (from mockups)

1. Show consumption (data dial, minutes, validity days, current network).
2. Top-ups / balance recharges.
3. Network diagnostics (SIM/data/APN/roaming/signal checklist, RSRP/SINR/band,
   cell id) feeding support tickets.
4. Promotions (banners, "doble de GB" style offers).
5. FAQs.
6. Invoice requests (CFDI).
7. Line linking (multi-line accounts).
8. Support hub: show SPN/APN config (name, APN, SMSC with copy buttons) and
   open the right OS settings screen when possible.

## Non-negotiable rules (already enforced by S0)

- Auth only through defdo_auth (public PKCE client, discovery-driven).
- The app never sends `brand_code`, `brand_key`, `app_key`, `theme_code`, or
  `tenant` as authority — backend derives context from the OAuth client +
  AccessContext.
- Tokens only in Keychain/Keystore; never logged.
- All `/mobile/*` calls carry only the bearer token.
- Theme from `/mobile/theme` with embedded fallback (done). Note: defdo_my_mvno
  already integrates **defdo_theme_hub** directly — theme does NOT depend on
  core_graph; when the BFF moves, `/mobile/theme` is served from theme_hub.

## Slices

Each slice is independently shippable: contract → backend (interim/final) →
Android → iOS → fixture tests in shared-contracts.

### S0 — Foundation (DONE)
Auth (PKCE, discovery fixtures, secure storage), `POST /mobile/bootstrap`,
`GET /mobile/theme`, app shells, AGP 9 / SPM toolchains, CI-able tests.

### S1 — Consumption dashboard (FIRST)

> **Contract status: DRAFT.** The real consumption API may differ (the
> canonical structure lives in the `core` repo that core_graph is based on).
> Fixtures in `shared-contracts/selfcare/` are a proposal for negotiation —
> do not wire live clients until defdo_my_mvno confirms the shape.
- Contract: `GET /mobile/usage` → active plan name, data used/total (per
  bucket: normal/social/promo), minutes/SMS state, days remaining, current
  network type, balance.
- Backend: **defdo_my_mvno** serves the endpoint; it reads usage/balance from
  core_graph server-side (M2M). Proxy per-request initially (fresh data),
  cache/sync later if latency demands it.
- Apps: home screen dial (14.2 GB style), buckets breakdown, "Datos OK / Voz
  OK" chips.
- Fixtures: `shared-contracts/selfcare/usage.success.fixture.json`,
  empty-plan, suspended-line variants.
- Acceptance: dial renders from fixture on both platforms; pull-to-refresh;
  cached last snapshot offline.

### S2 — Package catalog + detail
- Contract: `GET /mobile/catalog` → packages (price MXN, GB, vigencia, tags:
  recomendado/social/viajero), filters (duración/GB).
- Backend: **defdo_my_mvno** serves it; catalog data pulled from core_graph
  (sync/cache is fine here — catalog changes rarely).
- Apps: "Paquetes Disponibles" list + detail sheet. Purchase button disabled
  until S3.
- Acceptance: catalog renders from fixture; filter chips work; detail shows
  full package facts.

### S3 — Top-ups / purchase
- Contract: `POST /mobile/orders` (package_id) → order + payment session URL
  or token; `GET /mobile/orders/:id` for status polling.
- Backend: defdo_my_mvno orchestrates with defdo_checkout/defdo_payments.
  Payment UI = system browser / payment sheet, never card data in-app.
- Acceptance: happy path (paid → provisioned), pending, failed, idempotent
  retry. No PCI surface in the app.

### S4 — Network diagnostics + APN/SPN support
- On-device (no backend): SIM detected, data enabled, roaming state, signal.
  - Android: `TelephonyManager` (SPN via `getSimOperatorName()`,
    `READ_PHONE_STATE`), RSRP/SINR/band/cell id via `CellInfo` (needs
    `ACCESS_FINE_LOCATION` — degrade gracefully if denied). Open APN screen
    with `Settings.ACTION_APN_SETTINGS` intent.
  - iOS: radio details are NOT readable (CoreTelephony carrier info is
    deprecated/redacted since iOS 16) and APN settings cannot be opened
    programmatically. iOS shows: connectivity checklist (reachability, data
    path), the expected APN/SMSC values with copy buttons, and optionally a
    `.mobileconfig` APN profile download served by the backend.
- Backend: `GET /mobile/network-profile` → expected SPN, APN name, SMSC,
  roaming policy per brand (drives the "Copiar configuración" screen).
- Ticket handoff: `POST /mobile/tickets` with diagnostic payload
  (checklist results + metrics; NEVER tokens/identifiers beyond line ref).
- **Carrier-mandatory ticket fields: cell details (Cell ID / band) and
  coordinates (lat/lon).** Without them the provider cannot see the case and
  it has to be raised through alternate manual channels — the app must treat
  them as required:
  - Android: auto-captured — Cell ID/band from `CellInfo`, coordinates from
    fused location. Both behind runtime permission prompts; if the user
    denies location, block ticket submission with a clear "required by
    carrier" explanation and a re-prompt path.
  - iOS: coordinates auto-captured via CoreLocation (permission prompt).
    Cell ID/band have NO public API — not obtainable with any permission or
    entitlement. Field Test Mode (`*3001#12345#*`) is an Apple-internal app
    that cannot be launched or read programmatically (private APIs = App
    Store rejection). Ticket flow therefore includes a guided step:
    instructions to dial `*3001#12345#*`, where to find the serving Cell ID,
    and a validated manual entry field. The backend may additionally resolve
    the serving cell provider-side from MSISDN + timestamp + coordinates.
- Acceptance: checklist mirrors mockup; "Abrir Ticket de Soporte" refuses to
  submit without cell details + coordinates and explains why; Android
  auto-fills both; iOS auto-fills coordinates and walks the user through
  Field Test for the Cell ID; Android opens APN settings; iOS shows
  copy-config screen.

### S5 — Support hub + FAQs
- Contract: `GET /mobile/faqs` (sections, markdown answers, cacheable/ETag),
  `GET /mobile/tickets` + detail for follow-up.
- Backend: defdo_my_mvno (content may originate in defdo_cms).
- Acceptance: FAQ list offline-cached; ticket list shows status timeline.

### S6 — Promotions
- Contract: `GET /mobile/promotions` → banners (image URL, title, body,
  optional deeplink to catalog item), validity window.
- Apps: home carousel ("Promociones para ti") + "Ver todas".
- Acceptance: hidden when empty; images cached; deeplink lands on S2 detail.

### S7 — Invoices (CFDI)
- Contract: `POST /mobile/invoices` (order_id + fiscal data ref),
  `GET /mobile/invoices` list with PDF/XML links.
- Backend: defdo_my_mvno + sat_mexico. Fiscal data captured once, stored
  server-side; app never persists RFC locally beyond form state.
- Acceptance: request invoice for a paid order; list shows issued docs.

### S8 — Line linking (LAST)
- Contract: `POST /mobile/lines/link` (msisdn + OTP challenge),
  `GET /mobile/lines`, switch active line context.
- Explicitly out of scope until S1–S7 are stable (was also excluded from the
  discovery validation milestone).

## Cross-cutting backlog

- Runtime discovery wiring (fetch `discoveryURL` async at startup instead of
  synthesizing endpoints) — pending on both platforms; libraries ready.
- `swift test` in CI (host machines with CommandLineTools cannot run XCTest).
- Register the selfcare OAuth client on the live tenant (exact redirect URI,
  scopes `openid profile offline_access`).
- core_graph: `actor_apis` entry for the mobile client_id +
  `bootstrap_enabled`/`theme_enabled` actor metadata.
- Every new `/mobile/*` contract gets a fixture pair in `shared-contracts/`
  before platform work starts (same discipline as auth/theme).

## Sequencing

S1 → S2 start with the defdo_my_mvno BFF endpoints backed by core_graph data
(M2M read — core_graph work is limited to exposing/authorizing that read).
S4's on-device half has zero backend dependency and runs in parallel. S3
blocks on defdo_my_mvno order orchestration; S5–S7 are defdo_my_mvno-first;
S8 last.
