# AGENTS.md — Contributor & AI Agent Guidelines

This document outlines core principles, operational constraints, and workflow requirements for AI agents and human contributors working within the **Flipp** repository.

---

## 1. Core Mission & Architectural Tenets

Flipp is a **100% offline, privacy-first Android file converter and document utility**. Every technical decision must preserve three core tenets:
1. **Absolute Offline Privacy:** No network requests, no remote telemetry, no external SDKs, and **zero network permissions** in `AndroidManifest.xml`.
2. **On-Device Local Processing:** All conversions, merges, splits, watermarking, and text extractions execute directly on the user's Android hardware using native APIs (`PdfRenderer`, `Bitmap`, `Canvas`) and local libraries (`iTextG`).
3. **User-Controlled Storage via SAF:** Files are accessed and written exclusively via Android's Storage Access Framework (SAF: `OpenDocument`, `OpenMultipleDocuments`, `CreateDocument`). The app maintains no persistent internal document database and requests zero broad storage permissions.

---

## 2. Agent Behavior & Communication Expectations

- **Inspect Before Modifying:** Always inspect relevant source code, build files, and existing tests before proposing or writing changes. Never assume behavior based on filenames or directory structures alone.
- **Concise & Evidence-Based Communication:** Ground statements in actual source files and configuration. Provide exact file references and line numbers where appropriate.
- **Preserve Existing Documentation & Design:** Respect documented patterns in `REPORT.md`, `.Jules/bolt.md`, `.Jules/palette.md`, `DESIGN.md`, and `ARCHITECTURE.md`.
- **Do Not Guess or Invent Missing Features:** Do not invent speculative APIs, hypothetical dependencies, or unwarranted abstractions.
- **Request Approval for Destructive or Scope-Expanding Changes:** Do not refactor application architecture or add dependencies without explicit user consent.

---

## 3. Coding Standards & Implementation Practices

### Kotlin & Android
- **Language & Style:** Target Kotlin 2.2+ with official Kotlin code style (`kotlin.code.style=official` in `gradle.properties`).
- **Target Platform:** `minSdk = 26` (Android 8.0 Oreo), `targetSdk = 36`, `compileSdk = 36.1`, Java 11 bytecode compatibility.
- **Logging:** Use `AppLogger` (`com.example.AppLogger`) rather than bare `println` or unformatted `android.util.Log`. Never log sensitive user data, passwords, file paths, or file contents.
- **Resource Management:** Always wrap I/O streams and system descriptors (`InputStream`, `OutputStream`, `ParcelFileDescriptor`, `PdfRenderer`, `PdfReader`, `PdfStamper`) in Kotlin `.use { ... }` or explicit `try-finally` blocks.
- **Bitmap & Memory Management:**
  - Scale Bitmaps using `Matrix` transformations directly on `Canvas.drawBitmap` rather than creating intermediate allocations via `Bitmap.createScaledBitmap` (see `.Jules/bolt.md`).
  - Read dimensions first using `BitmapFactory.Options.inJustDecodeBounds = true` and calculate `inSampleSize` before decoding large bitmaps into memory.
  - Recycle transient bitmaps via `.recycle()` and free reader resources in `PdfCopy` via `.freeReader(reader)`.
  - Use `zipOut.setLevel(Deflater.NO_COMPRESSION)` when packaging pre-compressed formats like PNG into ZIP archives.

### Jetpack Compose & UI Components
- **Declarative Navigation:** Flipp uses single-activity screen-state navigation (`var currentScreen by rememberSaveable { mutableStateOf("home") }`) inside `MainActivity.kt`. Do not introduce Jetpack Navigation Compose or third-party routing libraries unless requested.
- **Component Reusability:** Favor reusable components from `UiComponents.kt`:
  - `BrutalistShadowBox`: Base neo-brutalist card with 2dp black border, 4dp solid shadow, and spring press animation.
  - `BrutalistButton`: Primary action button adhering to accessibility standards.
  - `TopNavbar` & `BottomTrustBar`: Global headers and trust indicators.
  - `ToolBadge`: Status or tool category badge.
  - `DropZone` & `SingleFileDropZone`: File picker trigger with dashed border.
  - `StageProgressBar`: Multi-stage progress indicator.
- **Neobrutalist Styling Conventions:**
  - High-contrast 2dp black borders (`#0D0D0D`).
  - Solid hard drop shadows (4dp offset, no blur).
  - Bold pastel palette (`YellowColor`, `SkyBlueColor`, `PinkColor`, `MintColor`, `AmberColor`, `CreamColor`).
  - Heavy typography weights (Black 900 for titles, Bold 700 for subtitles/buttons).
- **Accessibility Standards (WCAG 2.1 AA):**
  - Minimum touch target: All interactive elements must meet or exceed **48dp × 48dp** (use `IconButton` wrappers, not raw `Box.clickable`).
  - Radio & Multi-option grids: Use `Modifier.selectable(selected = ..., role = Role.RadioButton, onClick = ...)` instead of bare `.clickable`.
  - Icon-only elements: Provide dynamic and accurate `contentDescription` strings (e.g. "Show password" / "Hide password", not generic "Lock icon").
  - Form fields & sliders: Attach `.semantics { contentDescription = "..." }` when visual labels sit outside input bounds (see `.Jules/palette.md`).
  - Containers: Provide `role = Role.Button` and `onClickLabel` on clickable rows/containers.

---

## 4. Git & Version Control Practices

- **Atomic, Purpose-Driven Commits:** Group related changes logically. Use conventional commit formatting (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`).
- **Strict Clean Repository Standards:** Never check in build outputs (`build/`, `*.apk`, `*.aab`), keystores (`*.jks`, `*.keystore`, base64 files), environment files (`local.properties`), or OS-specific artifacts.
- **Release Versioning:**
  - Releases are triggered by pushing git tags conforming to `vX.Y.Z` (e.g., `v1.0.0`).
  - CI derives `versionCode` via `MAJOR * 10000 + MINOR * 100 + PATCH`.
  - Version codes must be strictly monotonically increasing; the release CI workflow enforces this check against all existing remote tags before building and publishing.

---

## 5. Testing & Verification Requirements

- **Test Frameworks:**
  - Unit & Behavior tests: JUnit 4 + Robolectric (`@Config(sdk = [34])`).
  - Compose UI tests: `createComposeRule()` from `androidx.compose.ui.test.junit4`.
  - Screenshot regression tests: Roborazzi.
- **Verification Rule:** Whenever modifying or adding a tool, utility, or UI component:
  1. Write or update corresponding unit/Robolectric tests under `app/src/test/java/com/example/`.
  2. Verify edge cases (empty inputs, zero-byte files, encrypted/password-protected PDFs, invalid page ranges, cancellation during file picking).
  3. Ensure UI accessibility semantics are verified using Compose test matchers (`hasContentDescription`, `isSelected`, `expectValue(SemanticsProperties.Role, ...)`).
- **CI Parity:** Local tests should pass `./gradlew testDebugUnitTest`.

---

## 6. Error Handling & Debugging

- **User-Facing Feedback:** Never leave the user hanging on a stalled progress indicator. Handle failure states explicitly by resetting stage state and displaying clear Toast notifications or error cards.
- **Coroutines & Threading:** Always offload file I/O and PDF/image processing to `Dispatchers.IO` or `Dispatchers.Default`. Switch back to `Dispatchers.Main` for UI state updates.
- **Safe Exceptions:** Catch specific exceptions (e.g., `SecurityException` for encrypted PDFs in `PdfRenderer`, `IllegalArgumentException` for invalid split ranges) and log via `AppLogger.e(TAG, message, e)`.

---

## 7. Security Precautions

- **Zero Network Infiltration:** Under no circumstances should `android.permission.INTERNET` or any network library (e.g., Retrofit, OkHttp, Ktor) be added to the project.
- **Keystore & Secret Safety:** Never hardcode passwords or cryptographic keys. The release workflow injects keystore credentials via GitHub Secrets (`KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`).
- **Data Protection:** `android:allowBackup` is strictly set to `"false"` in `AndroidManifest.xml` alongside explicit `data_extraction_rules.xml` and `backup_rules.xml`.
- **Encrypted PDF Handling:** When password-protecting PDFs, use standard 256-bit AES encryption (`PdfWriter.ENCRYPTION_AES_256`). When reading password-protected PDFs, catch `SecurityException` gracefully and guide the user.

---

## 8. Dependency & Architectural Discipline

- **Dependency Version Catalog:** All dependencies and plugins must be declared in `gradle/libs.versions.toml`. Do not declare direct string versions inside `app/build.gradle.kts`.
- **Minimal Footprint:** Avoid third-party library sprawl. Do not add libraries when native Android APIs or standard Kotlin libraries suffice.
- **Architectural Stability:** Do not replace the screen-state navigation or single-activity architecture with complex multi-module or heavyweight MVVM/MVI frameworks unless directed by project leadership.
