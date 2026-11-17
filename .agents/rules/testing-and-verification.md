---
trigger: always_on
description: "Testing and verification guidelines for JUnit 4, Robolectric, Compose UI, and CI in Flipp"
---

# Testing & Quality Verification

## 1. Test Architecture & Frameworks
- **Unit & Robolectric Tests:** Placed under `app/src/test/java/com/example/`. Use `@RunWith(RobolectricTestRunner::class)` and `@Config(sdk = [34])` for Android framework dependencies.
- **Compose UI Tests:** Use `createComposeRule()` from `androidx.compose.ui.test.junit4` to test UI components and screen interactions.
- **Screenshot Regression Tests:** Utilize Roborazzi (`libs.roborazzi`) for visual snapshot verification when modifying design system components.

## 2. Test Requirements for Changes
- **Domain Utilities:** Any new or modified function in `PdfUtils.kt` or `ImageUtils.kt` must have unit tests covering:
  - Happy path conversions and accurate output formats.
  - Zero-byte, empty, or corrupted input streams.
  - Edge cases (e.g., encrypted PDFs throwing `SecurityException`, invalid split page bounds).
  - Proper resource cleanup (stream closure, bitmap recycling).
- **UI & Accessibility:** Screen and component changes must include tests verifying:
  - Minimum touch target and container accessibility roles (`Role.Button`, `Role.RadioButton`).
  - Semantic content descriptions for external input labels (`OutlinedTextField`, `Slider`).
  - Dynamic accessibility state toggles (e.g. password visibility).
- **Manifest Constraints:** Changes to app configuration must pass manifest security tests (e.g. `allowBackup is configured to false`).

## 3. Verification Commands & CI Alignment
- Run JVM unit and Robolectric tests locally:
  ```bash
  ./gradlew testDebugUnitTest
  ```
- Before pushing release tags (`v*`), verify semantic version format `vMAJOR.MINOR.PATCH` and ensure derived `versionCode` (`MAJOR*10000 + MINOR*100 + PATCH`) strictly exceeds all previously published tags.
