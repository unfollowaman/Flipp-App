---
trigger: always_on
description: "Coding standards and architectural guidelines for Kotlin, Jetpack Compose, and Gradle in Flipp"
---

# Coding Standards & Architecture

## 1. Stack & Architecture
- **Language & Runtime:** Kotlin 2.2+, Target SDK 36, Min SDK 26, Java 11 compatibility.
- **Dependency Management:** Centralize all library and plugin declarations in `gradle/libs.versions.toml`. Never hardcode dependency strings in `app/build.gradle.kts`.
- **Navigation Pattern:** Single-activity Compose architecture managed via `rememberSaveable` screen state (`MainActivity.kt`). Do not introduce third-party routing or multi-activity flows unless explicitly directed.

## 2. Jetpack Compose & Neobrutalist UI
- **Design Tokens:** Always utilize colors from `com.example.ui.theme.Color` (`BlackColor`, `WhiteColor`, `CreamColor`, `MintColor`, `YellowColor`, etc.).
- **Component Reuse:** Build new screens and tools using primitives in `UiComponents.kt`:
  - `BrutalistShadowBox`: Standard card container with 2dp black border, 4dp solid shadow, and spring press animation.
  - `BrutalistButton`: Standard interactive button.
  - `DropZone` / `SingleFileDropZone`: System file picker triggers.
  - `StageProgressBar`: Semantic progress indicator.
- **Visual Constants:** Maintain 2dp black borders (`#0D0D0D`), 4dp unblurred drop shadows, and 10dp/16dp corner radii across all components.

## 3. Accessibility (WCAG 2.1 AA)
- **Touch Targets:** Minimum 48dp × 48dp on all interactive elements. Wrap icon-only buttons in `IconButton`.
- **Option Grids:** Use `Modifier.selectable(selected = isSelected, role = Role.RadioButton, onClick = ...)` for radio/selection groups instead of bare `.clickable`.
- **Screen Reader Labels:** Add `.semantics { contentDescription = "..." }` to `OutlinedTextField` and `Slider` components when visual labels are rendered outside input bounds.
- **Dynamic Descriptions:** Trailing toggle icons must communicate dynamic state (e.g. "Show password" vs. "Hide password").

## 4. Performance & Memory Management
- **Matrix Scaling:** Combine translation, rotation, and scaling into a single `Matrix` passed to `Canvas.drawBitmap`. Do not allocate intermediate scaled bitmaps via `Bitmap.createScaledBitmap`.
- **Bounds Pre-Decoding:** Always set `inJustDecodeBounds = true` on `BitmapFactory.Options` to calculate `inSampleSize` downsampling before decoding large image files or watermarks.
- **ZIP PNG Archiving:** Call `zipOut.setLevel(Deflater.NO_COMPRESSION)` when writing PNG bytes into ZIP streams to avoid redundant DEFLATE compression.
- **Resource Cleanup:** Always recycle temporary `Bitmap` buffers, close I/O streams using `.use { }`, and call `copy.freeReader(reader)` when merging PDFs with `PdfCopy`.
