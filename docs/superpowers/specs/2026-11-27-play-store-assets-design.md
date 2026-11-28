# Google Play Store Assets Remediation — Design

## Intent

Prepare upload-ready Google Play listing assets and copy for Flipp, using the Android app's actual features, existing mascot, and established visual identity. This is the asset-preparation scope for `FLIPP-P0-003`; it does not publish anything or change the app.

## Goals

- Convert the existing 512 × 512 store icon to RGBA PNG while preserving its logo and opaque yellow background.
- Create a 1024 × 500 feature graphic with Flipp's mascot and concise, verified PDF-tool messaging.
- Write Play short and full descriptions from the 16 tools listed in `MainActivity.kt`.
- Draft a deployable HTML privacy policy from the manifest, dependency catalog, backup rules, file-selection flows, cache usage, and logger implementation.
- Record asset validation, missing genuine screenshots, privacy-policy hosting, and all remaining actions in `release-audit/STORE_ASSETS_REMEDIATION.md`.

## Non-goals and constraints

- Do not change application behavior, package/version/signing settings, or dependencies.
- Do not upload or publish assets to Google Play. Commit or push only with explicit user authorization.
- Do not fabricate screenshots. No Android device or emulator is currently available, so provide a precise capture plan instead.
- Do not invent a public policy URL or developer identity/contact details. The policy will identify Flipp and direct inquiries to the developer contact shown on its Play listing.
- Disclose app-private cache files and local Android log diagnostics accurately; do not claim that no file is ever stored or that cleanup always succeeds.

## Approaches considered

1. **Minimal brand-led graphic (selected by the user):** use the existing mascot, established yellow/black/pastel palette, and a short product-purpose line. Gain: legible, brand-consistent message at store size; trade-off: shows fewer individual tools.
2. **Verified tool lineup:** place several actual tool names/icons around the brand. Gain: more breadth; trade-off: denser copy and weaker hierarchy at small sizes.
3. **App-screen collage:** use screenshots as the main graphic. Gain: shows product UI; trade-off: authentic screenshots cannot currently be captured and this would make the graphic depend on unavailable capture hardware.

The first approach best fits the repository's established mascot and bold yellow identity while avoiding fake UI and unsupported claims.

## Proposed deliverables and paths

- `store-assets/play-store-icon-512.png` — preserved artwork, 512 × 512 RGBA PNG.
- `store-assets/feature-graphic-1024x500.png` — opaque 1024 × 500 PNG, plus an editable SVG source alongside it.
- `store-assets/short-description.txt` and `store-assets/full-description.txt` — within current Play limits and limited to verified features.
- `store-assets/privacy-policy.html` — static, deployable policy; no hosted URL will be asserted.
- `release-audit/STORE_ASSETS_REMEDIATION.md` — itemized completion status, paths, validation findings, screenshot capture plan, and manual hosting/linking actions.

## Privacy facts to disclose

The source manifest declares no permissions. The merged release manifest adds one app-scoped AndroidX Core signature permission and no `INTERNET` permission; the resolved runtime dependencies contain no analytics, advertising, crash-reporting, or network-client SDK. The app reads user-selected inputs and saves chosen outputs through Android's Storage Access Framework. Some operations write temporary output files in app-private cache; code deletes files along workflow cleanup paths and sweeps only a subset of stale names at startup, so cleanup is best-effort and interruption may leave cache files. AppLogger writes diagnostics to Android's local system log. Backup is disabled and the extraction rules exclude app data. The policy will explain that Android or a selected third-party document provider has its own handling when the user selects a provider-backed location, and that PDF-to-text can copy text to the system clipboard when the user chooses that action.

## Validation and review limits

Validate PNG dimensions, encoding, color mode, and alpha-channel presence; verify feature-graphic dimensions/opaque color; count description characters; and check policy consistency against source. Better Design review calls failed with HTTP 402 because the available query quota is exhausted, so no Better Design review or approval can be claimed. Genuine phone screenshots and a publicly accessible HTTPS policy URL remain external requirements before the blocker is fully resolved.
