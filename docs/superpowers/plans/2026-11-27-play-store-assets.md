# Play Store Assets Remediation Implementation Plan

> **For agentic workers:** Use the native single-agent workflow in this session. Do not delegate. Commit or push only with explicit user authorization; do not publish assets to Google Play.

**Goal:** Prepare Flipp's Play listing icon, feature graphic, descriptions, and privacy-policy HTML, then document unresolved screenshot and hosting actions.

**Architecture:** Keep all listing files under `store-assets/` and the requested status report under `release-audit/`. Preserve the existing mascot, render a flat brand-led graphic from an editable SVG source, and author descriptions and policy only from verified Android source. Do not alter Android app code.

**Tech Stack:** Existing PNG artwork; SVG and Pillow for deterministic graphic output; UTF-8 plain text; semantic static HTML; PowerShell/Python checks. No new dependencies.

**Spec:** `docs/superpowers/specs/2026-11-27-play-store-assets-design.md`

## Global Constraints

- Preserve the current Flipp mascot and yellow/black/pastel Android identity.
- Keep the icon at 512 × 512 RGBA and the feature graphic at exactly 1024 × 500, opaque PNG.
- Do not fabricate Android UI screenshots; without a device/emulator, provide a capture plan.
- Do not invent app features, privacy practices, developer identity, or a public HTTPS URL.
- Do not change app behavior, application ID, version, signing, or dependencies.
- Do not upload or publish assets to Google Play. Commit or push only with explicit user authorization.
- Better Design returned HTTP 402 quota exhaustion; do not claim its review occurred.

## Review Focus

- Icon encoding: verify 512 × 512, PNG, 8-bit RGBA color type, alpha channel present, and intact preserved artwork.
- Feature graphic: verify 1024 × 500 PNG, RGB/opaque export, clear hierarchy, use of the actual mascot, and no mock UI.
- Listing copy: verify short description ≤80 characters, full description ≤4,000 characters, and every listed tool against `MainActivity.kt` and screen implementations.
- Privacy statements: verify SAF selection/export, private-cache scratch outputs and best-effort cleanup, Android system logs, no network permission/analytics SDK, clipboard behavior, and disabled backup.
- Screenshot status: confirm no physical device/AVD is available; do not generate substitutes; provide four real-screen capture scenarios and capture specifications.

---

### Task 1: Icon and feature graphic

**Files:**
- Modify: `store-assets/play-store-icon-512.png`
- Create: `store-assets/feature-graphic-1024x500.svg`
- Create: `store-assets/feature-graphic-1024x500.png`

**Interfaces:**
- Consumes: `store-assets/play-store-icon-512.png`, `app/src/main/res/drawable-nodpi/flipp_logo.png`, brand colors and type from `DESIGN.md` and `app/src/main/java/com/example/ui/theme/`.
- Produces: an icon retaining its visible pixels with RGBA encoding, and an editable SVG plus a flattened opaque PNG featuring the existing mascot and the line “PDF tools. On your device.”

- [x] Convert the current icon to 8-bit RGBA without altering its 512 × 512 artwork or yellow background.
- [x] Draw the chosen minimal composition in SVG using Flipp's existing mascot, palette, Bricolage Grotesque typography, and concise verified copy; keep the text beside the mascot.
- [x] Render the SVG to a 1024 × 500 opaque PNG with antialiasing and no extra UI.
- [x] Validate PNG signatures/IHDR, dimensions, color types, alpha presence, and image integrity; inspect both outputs visually.

### Task 2: Store copy and privacy policy

**Files:**
- Create: `store-assets/short-description.txt`
- Create: `store-assets/full-description.txt`
- Create: `store-assets/privacy-policy.html`

**Interfaces:**
- Consumes: `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, backup rules, `MainActivity.kt`, file selection/output flows, cache cleanup, and `AppLogger.kt`.
- Produces: copy naming only the 16 tool screens actually exposed by the Android dashboard; a static policy identifying Flipp and a privacy inquiry mechanism via the developer contact shown on its Play listing.

- [x] Write a short description about on-device PDF conversion/editing/organization/protection; count visible Unicode characters and keep it within 80.
- [x] Write a natural full description grouped by verified tool tasks and privacy behavior; keep it within 4,000 characters and avoid absolute claims about provider-controlled file locations.
- [x] Draft a semantic, deployable HTML privacy policy covering selected file contents/metadata and user inputs, local processing, SAF providers, exports, temporary cache and its best-effort cleanup, local Android logs, clipboard use, no network/analytics/crash SDK, disabled backup, retention, deletion, and contact.
- [x] Check both descriptions against the app inventory and policy statements against source; do not claim a hosted URL.

### Task 3: Remediation report and final validation

**Files:**
- Create: `release-audit/STORE_ASSETS_REMEDIATION.md`

**Interfaces:**
- Consumes: all assets and checks from Tasks 1–2, official Play limits, and device/AVD availability findings.
- Produces: a status for each requirement, exact paths/dimensions, exact store copy, privacy findings, four-screenshot capture plan, deployment/linking steps, and unresolved limitations.

- [x] Record the feature graphic as complete and the icon/descriptions as complete only after their checks pass.
- [x] Mark genuine screenshots blocked; give four authentic Android capture scenarios using generic sample files, portrait 9:16 captures from the running app, and the current Play file-size/dimension limits. Do not create screenshots now.
- [x] Mark policy hosting partially complete; give steps to publish the HTML on a public, globally accessible HTTPS page, verify it without login, set the listing URL, and make the URL accessible from within the app.
- [x] Recheck report paths and statuses against the actual workspace, `git status`, and every validation result; commit and push only with explicit user authorization, and do not publish assets to Google Play.
