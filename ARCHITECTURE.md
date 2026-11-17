# ARCHITECTURE.md — Flipp Technical Architecture & System Design

This document details the software architecture, data flow, component design, and build pipeline for the **Flipp** application.

---

## 1. High-Level System Overview

Flipp is architected as an **offline-only, single-activity Android application** built with **Jetpack Compose** and **Kotlin Coroutines**. It does not communicate with external servers, databases, or cloud endpoints.

```
┌────────────────────────────────────────────────────────┐
│                   Android OS Layer                     │
│  ┌───────────────────────┐   ┌──────────────────────┐  │
│  │ Storage Access        │   │ Native PdfRenderer / │  │
│  │ Framework (SAF)       │   │ Canvas / Bitmaps     │  │
│  └───────────▲───────────┘   └──────────▲───────────┘  │
└──────────────┼──────────────────────────┼──────────────┘
               │                          │
┌──────────────┼──────────────────────────┼──────────────┐
│ Flipp App    │                          │              │
│  ┌───────────▼──────────────────────────▼───────────┐  │
│  │ Domain / Engine Utilities                        │  │
│  │ - PdfUtils (iTextG + PdfRenderer)                │  │
│  │ - ImageUtils (Canvas Matrix Watermarking)        │  │
│  └───────────────────────▲──────────────────────────┘  │
│                          │                             │
│  ┌───────────────────────┴──────────────────────────┐  │
│  │ UI & Presentation (Jetpack Compose)              │  │
│  │ - MainActivity (Screen-State Navigation & Flows) │  │
│  │ - UiComponents (Neobrutalist Design System)      │  │
│  │ - ui/theme (Color, Type, Theme tokens)           │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

---

## 2. Technology Stack & Specifications

| Layer / Concern | Technology | Specification / Version |
| :--- | :--- | :--- |
| **Language** | Kotlin | `2.2.10` (Targeting Java 11 bytecode) |
| **Build System** | Gradle (Kotlin DSL) | `8.11.1` with Android Gradle Plugin `9.1.1` |
| **UI Toolkit** | Jetpack Compose | Compose BOM `2024.09.00`, Material 3 |
| **Platform Target** | Android SDK | `minSdk = 26` (8.0 Oreo), `targetSdk = 36`, `compileSdk = 36.1` |
| **Async Execution** | Kotlinx Coroutines | `1.10.2` (`Dispatchers.IO`, `Dispatchers.Main`) |
| **PDF Engine** | Android `PdfRenderer` + iTextG | Native Android framework + `com.itextpdf:itextg:5.5.10` |
| **Graphics Engine** | Android Graphics | `android.graphics.Bitmap`, `Canvas`, `Matrix`, `Paint` |
| **File I/O** | Storage Access Framework | System file picker contracts (`OpenDocument`, `CreateDocument`) |
| **Testing** | JUnit 4, Robolectric, Roborazzi | JUnit `4.13.2`, Robolectric `4.16.1` (SDK 34), Roborazzi `1.59.0` |

---

## 3. Repository Structure & Directory Map

```text
Flipp-App/
├── .github/
│   └── workflows/
│       └── release.yml               # Automated CI release & test workflow
├── .Jules/
│   ├── bolt.md                       # Performance & memory optimization logs
│   └── palette.md                    # Accessibility & Compose UI design rules
├── gradle/
│   ├── libs.versions.toml            # Version catalog (centralized dependencies)
│   └── wrapper/
│       ├── gradle-wrapper.jar        # Tracked Gradle binary distribution
│       └── gradle-wrapper.properties # Gradle wrapper configuration (8.11.1)
├── app/
│   ├── build.gradle.kts              # Application module build configuration
│   ├── proguard-rules.pro            # R8 / Proguard minification and keep rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # Manifest (Zero permissions, allowBackup=false)
│       │   ├── java/com/example/
│       │   │   ├── AppLogger.kt      # Unified logging wrapper
│       │   │   ├── ImageUtils.kt     # Canvas watermark & matrix transforms
│       │   │   ├── MainActivity.kt   # App entry point, navigation router & 9 screens
│       │   │   ├── PdfUtils.kt       # Local PDF operations engine
│       │   │   ├── UiComponents.kt   # Neobrutalist design system components
│       │   │   └── ui/theme/         # Design tokens (Color.kt, Theme.kt, Type.kt)
│       │   └── res/                  # App icon, color resources, and backup rules
│       └── test/                     # Unit, Robolectric, and accessibility tests
├── AGENTS.md                         # Contributor and AI agent guidelines
├── ARCHITECTURE.md                   # System architecture and technical design
├── DESIGN.md                         # Visual identity, typography, and styling guide
├── PRODUCT.md                        # Product vision, features, and user journeys
├── README.md                         # Project setup, overview, and commands
├── REPORT.md                         # Historical security and cleanup audit notes
├── settings.gradle.kts               # Gradle project and repository settings
├── build.gradle.kts                  # Root project build file
└── gradle.properties                 # JVM arguments, caching, and compiler flags
```

---

## 4. Major Modules & Architectural Responsibilities

### 4.1. Presentation Layer (`MainActivity.kt` & `UiComponents.kt`)
- **`MainActivity.kt`:** Serves as the single Android entry point (`ComponentActivity`). It contains:
  - `MainApp()`: Hosts the root `Scaffold`, top navigation bar, bottom trust bar, and back press handler.
  - State-based screen switching (`currentScreen` string state: `"home"`, `"privacy"`, `"pdf_png"`, `"image_pdf"`, etc.).
  - 9 individual tool composables managing multi-stage local UI state (`stage`: 1=Ingestion, 2=Config/Preview, 3=Processing, 4=Success).
  - Background thumbnail loaders (`loadPdfThumbnails`, `loadImageThumbnail`) for reactive document previews.
- **`UiComponents.kt`:** Encapsulates the Neobrutalist design system:
  - `BrutalistShadowBox`: Foundation container providing a 2dp black border, 4dp solid shadow, and spring-physics press animation.
  - `BrutalistButton`: Standard interactive action button.
  - `TopNavbar` & `BottomTrustBar`: Global trust and branding bars.
  - `DropZone` & `SingleFileDropZone`: System file picker triggers with dashed borders.
  - `StageProgressBar`: Semantic progress indicator.

### 4.2. Local PDF Domain Engine (`PdfUtils.kt`)
Encapsulates all PDF manipulations in pure Kotlin functions:
- `convertPdfToPngZip`: Uses Android `PdfRenderer` to rasterize pages into reusable `Bitmap` buffers, compressing directly into a `ZipOutputStream` with `Deflater.NO_COMPRESSION` to avoid redundant CPU cycles.
- `combineImagesToPdf`: Leverages `iTextG` to generate PDF documents from image byte arrays with `A4`, `Letter`, or `Auto` aspect ratios.
- `mergePdfs`: Merges multiple PDF input streams into one document via `PdfCopy`, actively freeing reader resources with `copy.freeReader(reader)`.
- `splitPdf`: Extracts designated page ranges `[startPage..endPage]` into a new PDF using `PdfCopy`.
- `protectPdf`: Applies 256-bit AES encryption (`PdfWriter.ENCRYPTION_AES_256`) to a source document with a user-supplied password.
- `addPageNumbers`: Uses `PdfStamper` and `BaseFont.HELVETICA_BOLD` to overlay page numbers on any of 6 predefined anchor positions.
- `textToPdf`: Formats raw text strings into multi-page PDF documents with customizable margins, font sizes, and alignments.
- `extractTextFromPdf`: Uses `PdfTextExtractor.getTextFromPage` to read digital text streams across all pages.

### 4.3. Local Image Domain Engine (`ImageUtils.kt`)
- `addWatermark`: Applies text or image watermarks directly onto source bitmaps.
  - **Memory Efficiency:** Uses `BitmapFactory.Options.inJustDecodeBounds = true` and `inSampleSize` downsampling before decoding overlay images.
  - **Single-Pass Rendering:** Transforms, scales, and rotates overlays directly via `android.graphics.Matrix` within `Canvas.drawBitmap`, eliminating intermediate bitmap allocations.

### 4.4. Logging Abstraction (`AppLogger.kt`)
Wraps `android.util.Log` to provide standard logging (`e`, `d`, `i`, `w`) while safeguarding against accidental string formatting crashes or unhandled throwable prints.

---

## 5. Data Flow & Execution Model

The application follows a strictly **unidirectional, stateless data flow**:

```
[ User Interaction ] 
        │ (Taps Browse)
        ▼
[ SAF Document Picker ] ──► Returns Uri(s)
        │
        ▼
[ Composable State Update ] (Sets Stage 2, calculates page count / thumbnails)
        │ (Taps Process)
        ▼
[ Coroutine Scope (Dispatchers.IO) ]
        │ ── Opens InputStream via ContentResolver
        │ ── Executes conversion in PdfUtils / ImageUtils
        │ ── Emits onProgress(current, total) callbacks
        ▼
[ Composable State Update (Dispatchers.Main) ] (Updates StageProgressBar)
        │ (Processing Complete)
        ▼
[ SAF CreateDocument Picker ] ──► Returns Destination Uri
        │
        ▼
[ Coroutine Scope (Dispatchers.IO) ] ── Writes final bytes to destination OutputStream
        │
        ▼
[ Success Screen (Stage 4) ]
```

### Key Principles of Flipp's Data Flow:
1. **No Intermediate Cloud or Disk Retention:** Data flows from the source `Uri` stream through memory (or temporary scratch files in the app's cache directory during multi-step processing) directly into the user's destination `Uri` stream.
2. **Explicit User Destination:** Files are only exported when the user explicitly triggers and confirms a save action via the system save dialog.
3. **Automatic Cleanup:** Temporary files created in `context.cacheDir` (such as temporary encrypted PDFs or ZIP outputs) are deleted immediately upon workflow completion or cancellation.

---

## 6. Network, APIs & Database Architecture

- **Network APIs:** **None.** Flipp contains zero network clients, zero REST/GraphQL integrations, and zero web sockets. The application manifest omits `android.permission.INTERNET`.
- **Database Architecture:** **None.** Flipp maintains no internal database (no SQLite, Room, Realm, or ObjectBox). Document metadata is read dynamically from Android `ContentResolver` on demand and discarded upon leaving the screen.
- **State Persistence:** UI screen state is preserved during device rotation using Compose's `rememberSaveable`.

---

## 7. Security Architecture & Privacy Boundaries

1. **Manifest Isolation:**
   - `android:allowBackup="false"`: Backups and cloud sync of application data are explicitly disabled.
   - `data_extraction_rules.xml` & `backup_rules.xml`: Disallow cloud and device-to-device data extraction.
2. **Storage Access Isolation:**
   - Flipp does not request `android.permission.READ_EXTERNAL_STORAGE` or `WRITE_EXTERNAL_STORAGE`.
   - Access to files is strictly scoped by the OS via temporary read/write URI permissions granted by SAF contracts.
3. **Cryptographic Standards:**
   - Password encryption employs AES-256 via iTextG (`PdfWriter.ENCRYPTION_AES_256`).
4. **Log Privacy:**
   - `AppLogger` logs only system lifecycle messages and error traces. No user text, file names, or URI paths are printed.

---

## 8. Build, Release & Deployment Pipeline

The project uses Gradle with GitHub Actions for automated continuous integration and release delivery:

### 8.1. Build Types & Signing
- **Debug Build (`debug`):** Configured with `debugConfig` signing. Uses local debug keystore.
- **Release Build (`release`):**
  - Minification enabled: `isMinifyEnabled = true` (R8 code shrinking and optimization).
  - Resource shrinking enabled: `isShrinkResources = true`.
  - PNG crunching disabled: `isCrunchPngs = false` (avoids corrupting pre-optimized assets).
  - Keystore parameters injected via environment variables: `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`.

### 8.2. CI/CD Release Pipeline (`.github/workflows/release.yml`)
1. **Trigger:** Triggered automatically when a tag matching `v*` (e.g. `v1.0.0`) is pushed.
2. **Environment:** Ubuntu runner with Temurin JDK 17.
3. **Verification:**
   - Validates semantic versioning syntax `vMAJOR.MINOR.PATCH`.
   - Derives integer `versionCode` using `MAJOR * 10000 + MINOR * 100 + PATCH`.
   - Fetches remote tags and enforces strict monotonic version ordering (fails if new `versionCode` ≤ any existing release).
   - Executes `./gradlew testDebugUnitTest`.
4. **Artifact Generation:**
   - Builds signed release APK (`assembleRelease`) and Android App Bundle (`bundleRelease`).
   - Copies outputs to `Flipp-vX.Y.Z.apk` and `Flipp-vX.Y.Z.aab`.
   - Publishes assets to GitHub Releases with auto-generated release notes.
