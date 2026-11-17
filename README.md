# Flipp — Privacy-First File Converter for Android

**Flipp turns everyday PDF and image chores into a fast, offline, tap-through Android experience.** Convert PDFs to images, build PDFs from photos, merge or split documents, protect files, add page numbers, stamp watermarks, and extract text — all without accounts, uploads, tracking, or a server in the middle.

If a first-time user asks, “Why should I install this?”, the answer is simple: **Flipp keeps powerful file conversion on the device where the files already live.** It is built for people who need reliable document tools on the go but do not want to hand private files to remote web converters.

---

## Documentation Index

Comprehensive technical and design documentation is maintained across the following documents:

- **[AGENTS.md](AGENTS.md):** Contributor and AI agent development rules, coding standards, testing requirements, and safety protocols.
- **[PRODUCT.md](PRODUCT.md):** Product purpose, target personas, problem space, capabilities, and future roadmap.
- **[ARCHITECTURE.md](ARCHITECTURE.md):** System architecture, data flow, component breakdown, security boundaries, and CI/CD pipeline.
- **[DESIGN.md](DESIGN.md):** Neobrutalist design system, color palette, typography, reusable UI components, and accessibility standards.
- **[REPORT.md](REPORT.md):** Historical repository security audit and cleanup report.

---

## What Flipp Does

Flipp is an Android file-conversion toolbox focused on **local-only document processing**. It presents each utility as a clear workflow: choose a file, configure options, preview or review settings, process locally, and save the result back to the device through Android’s Storage Access Framework (SAF).

### Core Features

| Feature | What it does | Output |
| :--- | :--- | :--- |
| **PDF → PNG** | Converts every page of a PDF into high-quality PNG images with 1x, 2x, or 3x scale options. | `.zip` containing page images |
| **Images → PDF** | Combines selected images into a single PDF with A4, Letter, or auto-sized pages. | `.pdf` |
| **Merge PDFs** | Combines multiple PDFs into one document in the selected order. | `.pdf` |
| **Split PDF** | Extracts a specific page range from a PDF into a separate document. | `.pdf` |
| **Protect PDF** | Adds 256-bit AES password encryption to a PDF. | `.pdf` |
| **Page Numbers** | Stamps page numbers onto a PDF with configurable starting number and placement. | `.pdf` |
| **Add Watermark** | Adds a text or image watermark to an image with position, opacity, size, rotation, and color controls. | Image file (`.png` / `.jpg`) |
| **Text → PDF** | Converts typed or pasted text into a PDF with page size, margin, font size, and alignment options. | `.pdf` |
| **PDF → Text** | Extracts selectable text from a PDF and lets users copy to clipboard or export. | `.txt` / clipboard |

### Privacy & Trust Features

- **Zero Network Permissions:** `AndroidManifest.xml` omits `android.permission.INTERNET`. The app cannot connect to the internet or transmit telemetry.
- **No Uploads:** All files are processed exclusively within device memory.
- **No Accounts Required:** Zero sign-up flows, logins, email requests, or profile tracking.
- **Offline-Native:** Conversion logic uses native Android framework APIs and bundled local libraries.
- **Storage Access Framework (SAF):** Reads and writes only user-selected files without requiring broad device storage permissions.
- **Backup Disabled:** `android:allowBackup="false"` prevents document leakage via cloud backups.

---

## Tech Stack

### Android App
- **Language:** Kotlin 2.2.10
- **UI:** Jetpack Compose with Material 3 (Compose BOM `2024.09.00`)
- **Architecture:** Single-activity Compose app with screen-state navigation
- **Minimum SDK:** Android 8.0 / API 26
- **Target SDK:** API 36
- **Compile SDK:** API 36 (minorApiLevel = 1)
- **Java Compatibility:** Java 11 bytecode compatibility (built with JDK 17)
- **Build System:** Gradle 8.11.1 (Kotlin DSL)
- **Android Gradle Plugin:** AGP 9.1.1
- **Dependency Management:** Centralized Version Catalog (`gradle/libs.versions.toml`)

### Local Processing Engine
- **Android `PdfRenderer`:** Native page rasterization to bitmaps for PDF-to-image workflows and page thumbnail rendering.
- **iTextG (`com.itextpdf:itextg:5.5.10`):** Powers PDF creation, merging, splitting, 256-bit AES encryption, page numbering, and text extraction.
- **Android Graphics (`Bitmap`, `Canvas`, `Matrix`, `Paint`):** Powers image watermark composition with single-pass matrix transformations and decode bounds downsampling.
- **Kotlin Coroutines (`1.10.2`):** Asynchronous background thread execution (`Dispatchers.IO`) with reactive progress updates.

### Testing & Quality Tools
- **JUnit 4 (`4.13.2`):** Core unit test runner.
- **Robolectric (`4.16.1`):** JVM-based Android behavior tests targeting API 34.
- **Compose UI Test:** Jetpack Compose testing rules and accessibility semantics verification.
- **Roborazzi (`1.59.0`):** Screenshot regression testing framework.
- **AndroidX Test:** Test runner, rules, and core Android testing utilities.

---

## Project Structure Map

```text
Flipp-App/
├── .github/
│   └── workflows/
│       └── release.yml               # Automated CI release & test workflow
├── .Jules/
│   ├── bolt.md                       # Performance & memory optimization logs
│   └── palette.md                    # Accessibility & Compose UI design rules
├── gradle/
│   ├── libs.versions.toml            # Centralized dependency and plugin versions
│   └── wrapper/
│       ├── gradle-wrapper.jar        # Gradle binary wrapper
│       └── gradle-wrapper.properties # Wrapper configuration
├── app/
│   ├── build.gradle.kts              # App module config, dependencies, signing, tests
│   ├── proguard-rules.pro            # ProGuard/R8 code and resource shrinking rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # App manifest (Zero permissions, launcher activity)
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt   # Compose entry point, navigation router, and 9 tool screens
│       │   │   ├── PdfUtils.kt       # Local PDF operations engine
│       │   │   ├── ImageUtils.kt     # Local image watermark processing
│       │   │   ├── UiComponents.kt   # Shared Neobrutalist Compose components
│       │   │   ├── AppLogger.kt      # Safe logging abstraction
│       │   │   └── ui/theme/         # Design tokens (Color, Theme, Typography)
│       │   └── res/                  # Drawables, mipmaps, strings, and backup rules
│       ├── test/                     # JVM, unit, Robolectric, and accessibility tests
│       └── androidTest/              # Instrumented device tests
├── AGENTS.md                         # Contributor and agent instructions
├── ARCHITECTURE.md                   # Complete architectural documentation
├── DESIGN.md                         # Visual identity and UI design system
├── PRODUCT.md                        # Product vision and specifications
├── README.md                         # Project overview and instructions
├── REPORT.md                         # Historical security cleanup report
├── settings.gradle.kts               # Gradle settings and repository configurations
├── build.gradle.kts                  # Root Gradle build script
└── gradle.properties                 # Gradle JVM arguments and build flags
```

---

## Getting Started

### Prerequisites

- **Android Studio:** Ladybug (2024.2.1) or newer recommended.
- **JDK:** JDK 17 (required by Gradle 8.11 and AGP 9.1).
- **Android SDK:** Installed with SDK Platform for API 36 and build-tools.
- **Device / Emulator:** An Android device or emulator running Android 8.0 (API 26) or higher.

### Local Development in Android Studio

1. Clone the repository:
   ```bash
   git clone https://github.com/YourUsername/Flipp-App.git
   cd Flipp-App
   ```
2. Open **Android Studio**.
3. Select **Open** and choose the `Flipp-App` root directory.
4. Allow Gradle to sync dependencies and download Android SDK build components.
5. Select the `app` run configuration and click **Run** (or press `Shift + F10`) targeting an emulator or connected device.

---

## Build Commands

All commands are executed using the Gradle wrapper (`./gradlew` on Linux/macOS or `.\gradlew.bat` on Windows).

### Development (Debug) Build
Compile and generate the debug APK:
```bash
./gradlew assembleDebug
```
The generated APK is located at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

### Production (Release) Build
To build release APK and Android App Bundle (AAB), pass the version properties and set signing environment variables:
```bash
# On Linux/macOS
export KEYSTORE_PATH="my-upload-key.jks"
export STORE_PASSWORD="your-keystore-password"
export KEY_PASSWORD="your-key-password"

./gradlew assembleRelease bundleRelease -PversionCode=10000 -PversionName=1.0.0

# On Windows PowerShell
$env:KEYSTORE_PATH="my-upload-key.jks"
$env:STORE_PASSWORD="your-keystore-password"
$env:KEY_PASSWORD="your-key-password"

.\gradlew.bat assembleRelease bundleRelease -PversionCode=10000 -PversionName=1.0.0
```
Artifacts are generated at:
- **APK:** `app/build/outputs/apk/release/app-release.apk`
- **AAB:** `app/build/outputs/bundle/release/app-release.aab`

---

## Testing & Quality Verification

### Run Unit and Robolectric Tests
Flipp includes a comprehensive suite of unit, Robolectric, and accessibility tests:
```bash
./gradlew testDebugUnitTest
```
Test results and HTML reports are generated under:
```text
app/build/reports/tests/testDebugUnitTest/index.html
```

### Run Connected Device Tests
To run instrumented tests on an attached device or running emulator:
```bash
./gradlew connectedAndroidTest
```

---

## Deployment & CI/CD Release Pipeline

Flipp includes an automated GitHub Actions release workflow defined in `.github/workflows/release.yml`.

### Automated Release Workflow:
1. **Trigger:** Create and push a semantic version tag:
   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```
2. **Automated Steps:**
   - Validates that the tag matches `vX.Y.Z`.
   - Derives `versionCode` using `MAJOR * 10000 + MINOR * 100 + PATCH`.
   - Enforces monotonic version ordering against all prior release tags.
   - Runs `./gradlew testDebugUnitTest`.
   - Decodes release keystore from GitHub Secret `KEYSTORE_BASE64`.
   - Compiles and signs `Flipp-vX.Y.Z.apk` and `Flipp-vX.Y.Z.aab`.
   - Creates a new GitHub Release with release assets and auto-generated release notes.

---

## Current Technical Limitations & Boundaries

- **Digital-Only Text Extraction:** The PDF-to-Text utility extracts embedded vector text streams. Scanned, photographed, or purely bitmap PDFs require OCR, which is not currently bundled.
- **On-Device Memory Capacity:** Because all processing occurs in local RAM, manipulating massive documents (e.g. 500+ pages or multiple 50MP images) is constrained by available device memory.
- **No Cloud Synchronization:** Flipp purposefully includes no cloud synchronization, account backend, or tracking.
