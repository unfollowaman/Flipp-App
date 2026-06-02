# Flipp — Privacy-First File Converter for Android

**Flipp turns everyday PDF and image chores into a fast, offline, tap-through Android experience.** Convert PDFs to images, build PDFs from photos, merge or split documents, protect files, add page numbers, stamp watermarks, and extract text — all without accounts, uploads, tracking, or a server in the middle.

If a first-time user asks, “Why should I install this?”, the answer is simple: **Flipp keeps powerful file conversion on the device where the files already live.** It is built for people who need reliable document tools on the go but do not want to hand private files to random web converters.

---

## What Flipp Does

Flipp is an Android file-conversion toolbox focused on **local-only document processing**. It presents each utility as a clear workflow: choose a file, configure options, preview or review settings, process locally, and save the result back to the device through Android’s document picker.

### Core Features

| Feature | What it does | Output |
| --- | --- | --- |
| **PDF → PNG** | Converts every page of a PDF into high-quality PNG images with 1x, 2x, or 3x scale options. | `.zip` containing page images |
| **Images → PDF** | Combines selected images into a single PDF with A4, Letter, or auto-sized pages. | `.pdf` |
| **Merge PDFs** | Combines multiple PDFs into one document in the selected order. | `.pdf` |
| **Split PDF** | Extracts a specific page range from a PDF into a separate document. | `.pdf` |
| **Protect PDF** | Adds password protection/encryption to a PDF. | `.pdf` |
| **Page Numbers** | Stamps page numbers onto a PDF with configurable starting number and placement. | `.pdf` |
| **Add Watermark** | Adds a text or image watermark to an image with position, opacity, size, rotation, and color controls. | Image file |
| **Text → PDF** | Converts typed text into a PDF with page size, margin, font size, and alignment options. | `.pdf` |
| **PDF → Text** | Extracts selectable text from a PDF and lets users copy or save it. | `.txt` / clipboard |

### Privacy & Trust Features

- **No uploads:** files are processed on the Android device.
- **No account required:** no sign-up flow, email, profile, or login dependency.
- **Offline-friendly:** conversion logic uses native Android APIs and bundled libraries instead of a hosted conversion service.
- **Document picker integration:** users choose and save files through Android system file APIs.
- **User-controlled outputs:** generated files are saved only when the user picks a destination.

---

## Tech Stack

### Android App

- **Language:** Kotlin
- **UI:** Jetpack Compose with Material 3
- **Architecture:** Single-activity Compose app with screen-state navigation
- **Minimum SDK:** Android 8.0 / API 26
- **Target SDK:** API 36
- **Compile SDK:** API 36.1
- **Java compatibility:** Java 11
- **Build system:** Gradle Kotlin DSL
- **Dependency management:** Gradle version catalog (`gradle/libs.versions.toml`)

### Local Processing “Backend” / Engine

Flipp does not rely on a remote backend service. Its conversion backend is the **on-device processing layer** inside the Android app:

- **Android `PdfRenderer`** renders PDF pages into bitmaps for PDF-to-image workflows and page-count inspection.
- **iTextG (`com.itextpdf:itextg`)** powers PDF creation, merging, splitting, encryption, stamping page numbers, and text extraction.
- **Android graphics APIs** (`Bitmap`, `Canvas`, `Paint`, `Matrix`) power image watermark composition.
- **Android Storage Access Framework** (`OpenDocument`, `OpenMultipleDocuments`, `CreateDocument`) handles file selection and export.
- **Kotlin coroutines** move conversion work off the main UI thread and update progress during processing.

### Testing & Quality Tools

- **JUnit 4** for unit tests.
- **AndroidX Test / Espresso** for Android test support.
- **Robolectric** for JVM-based Android behavior tests.
- **Roborazzi** for screenshot-style Compose regression testing.
- **Compose UI Test** for UI test infrastructure.

---

## How the App Works

1. **Choose a tool** from the home screen grid.
2. **Select input files** using Android’s system document picker.
3. **Configure options** such as scale, page size, page range, password, alignment, watermark opacity, or output layout.
4. **Process locally** using Android PDF/image APIs and iTextG.
5. **Save the result** through Android’s system save dialog.

Because files stay local, Flipp is especially useful for sensitive documents such as contracts, IDs, invoices, academic papers, personal notes, and internal business files.

---

## Project Structure Map

```text
Flipp-App/
├── README.md                         # Project overview, setup, features, and architecture
├── REPORT.md                         # Additional project/report notes
├── settings.gradle.kts               # Gradle project settings and repository configuration
├── build.gradle.kts                  # Root Gradle plugin declarations
├── gradle.properties                 # Gradle, Kotlin, caching, and build performance settings
├── gradlew / gradlew.bat             # Gradle wrapper scripts
├── gradle/
│   └── libs.versions.toml            # Centralized dependency and plugin versions
├── app/
│   ├── build.gradle.kts              # Android app module config, dependencies, signing, tests
│   ├── proguard-rules.pro            # ProGuard/R8 rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # App manifest and launcher activity
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt   # Compose app entry, navigation, and tool screens
│       │   │   ├── PdfUtils.kt       # Local PDF conversion, merge, split, protect, stamp, text utilities
│       │   │   ├── ImageUtils.kt     # Local image watermark processing
│       │   │   └── UiComponents.kt   # Shared Compose UI components and layout helpers
│       │   └── res/
│       │       ├── drawable/         # Launcher/background drawable resources
│       │       ├── mipmap-*/         # Launcher icons
│       │       ├── values/           # Colors, strings, and themes
│       │       └── xml/              # Backup and data extraction rules
│       ├── test/                     # JVM/unit/Robolectric/Roborazzi tests
│       └── androidTest/              # Instrumented Android tests
```

---

## Key Source Areas

- **`MainActivity.kt`** contains the single-activity Compose application, home screen, privacy screen, navigation, and all tool workflows.
- **`PdfUtils.kt`** is the local PDF engine for rendering, combining images into PDFs, merging, splitting, protecting, page numbering, text-to-PDF, and PDF text extraction.
- **`ImageUtils.kt`** handles watermark rendering for text and image stamps.
- **`UiComponents.kt`** provides shared UI building blocks such as the top nav, trust bar, buttons, badges, drop zones, progress bars, and brutalist-styled cards.
- **`gradle/libs.versions.toml`** records the Android Gradle Plugin, Kotlin, Compose, iTextG, Robolectric, Roborazzi, and AndroidX dependency versions.

---

## Getting Started

### Prerequisites

- Android Studio
- JDK compatible with the Android Gradle Plugin used by the project
- Android SDK with API 36 installed
- An Android emulator or physical Android device

### Run Locally

1. Open **Android Studio**.
2. Select **Open** and choose this project directory.
3. Let Android Studio sync Gradle and install any missing SDK components.
4. Run the `app` configuration on an emulator or physical device.

### Command-Line Build

```bash
./gradlew assembleDebug
```

The debug APK is generated under:

```text
app/build/outputs/apk/debug/
```

### Run Tests

```bash
./gradlew test
```

For connected-device tests, start an emulator or connect a device first, then run:

```bash
./gradlew connectedAndroidTest
```

---

## Release Signing Notes

The app module defines release signing through environment variables:

- `KEYSTORE_PATH` — optional path to the release keystore; defaults to `my-upload-key.jks` in the project root.
- `STORE_PASSWORD` — release keystore password.
- `KEY_PASSWORD` — release key password.

Debug builds use the configured debug signing setup.

---

## Current Limitations

- PDF-to-text extracts embedded/selectable text only. Scanned image-only PDFs require OCR, which is not currently included.
- File conversion happens on-device, so very large PDFs or images depend on available device memory and CPU performance.
- The app currently focuses on local conversion workflows rather than cloud sync, collaboration, or account-based file history.

---

## Why Flipp Stands Out

Most online converters ask users to upload private files first and trust the website later. Flipp flips that model: **the conversion happens locally, the interface stays simple, and the user keeps control of every file from start to finish.**
