# FEATURE_PAGES_REDESIGN_PLAN.md — Flipp Mobile Feature Pages Redesign

> **Role & Authority:** Lead Mobile Product & UI/UX Designer  
> **Target Project:** Flipp Android (`Flipp-App`)  
> **Visual Benchmark:** High-fidelity captures in [`Web Screenshots/`](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots)  
> **Date:** November 2026  
> **Status:** Approved for Phased Implementation  

---

## Executive Summary & Design Vision

Flipp's web version sets a high benchmark for **playful, tactile, and highly intuitive Neobrutalist design**. Its core signature is a deterministic **3-Stage Lifecycle**:
1. **Stage 1 (Initial / Idle):** Clear, uncluttered drop zone, bold badge branding, and structured "How-to" guides.
2. **Stage 2 (Loaded / Options & Preview):** High-density tactile pill controls (format, scale, range), document metadata chips, and rendered thumbnail grids.
3. **Stage 3 (Output / Results & Actions):** High-contrast celebration banners (Mint `✓ Conversion complete!`), primary bulk export button (`Download All as ZIP`), individual page output cards with discrete actions, and restart triggers.

The native Android app (`Flipp-App`) currently implements 9 tools with basic Compose radio buttons and simplified layouts, while 7 tools from the web platform are not yet built.

This plan outlines the complete **Phase-by-Phase Redesign and Harmonization Plan** to elevate all 16 feature pages into a unified, tactile, 60fps native mobile experience that matches and exceeds the web app while honoring mobile-first ergonomics.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          FLIPP 3-STAGE LIFECYCLE                            │
│                                                                             │
│  ┌───────────────────────┐   ┌───────────────────────┐   ┌───────────────┐  │
│  │   STAGE 1: IDLE       │   │   STAGE 2: LOADED     │   │ STAGE 3: DONE │  │
│  │  - Top Tool Badge     │   │  - Document Info Chip │   │ - Mint Banner │  │
│  │  - Dashed DropZone    │──▶│  - Tactile Pill Groups│──▶│ - Primary Save│  │
│  │  - Browse Files CTA   │   │  - Page Thumbnails    │   │ - Page Cards  │  │
│  │  - Educational Guide  │   │  - Colored Action CTA │   │ - Reset Action│  │
│  └───────────────────────┘   └───────────────────────┘   └───────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 1. Design System & Component Kit Extensions

Before redesigning individual screens, the core Compose component library (`UiComponents.kt`) will be extended with the following reusable neobrutalist primitives matching the web screenshots:

### 1.1. `NeobrutalistPillSelector`
- **Replaces:** Material3 radio buttons and standard dropdowns.
- **Visuals:** Horizontal or wrapped row of solid pill buttons. 
  - *Inactive:* White background, 2dp `#0D0D0D` border, subtle 2dp offset shadow, medium text.
  - *Active:* Solid `#0D0D0D` background with White text (or Saturated Accent Color: Yellow/Pink/Mint with Black text).
  - *Subtext Pill:* Support for secondary badge descriptors (e.g. `PNG — Best quality` vs `JPG — Smaller size`).
- **Touch Targets:** Minimum 48dp height with 8dp tap padding.
- **Haptics:** `HapticFeedbackType.TextHandleMove` (subtle tick) on selection.

### 1.2. `DocumentSummaryChip`
- **Visuals:** Cream background (`#FAFAE8`), rounded 10dp corner, 2dp black border, 4dp hard shadow.
- **Contents:** Document icon + bold page count + truncated filename string + file size chip (`5.62 MB`).

### 1.3. `ThumbnailPreviewGrid`
- **Visuals:** 2-column adaptive `LazyVerticalGrid` showing rendered pages.
- **Card Framing:** White surface, 2dp black border, 4dp shadow, rounded 10dp corner.
- **Badges:** Bottom label (`Page 1`, `Page 2`) with integrated remove/reorder action handles.
- **Memory Safety:** Render thumbnails at fixed 0.3x–0.5x scale in `Dispatchers.IO` with automatic bitmap recycling.

### 1.4. `ResultsHeroBanner`
- **Visuals:** Mint green background (`#3DD68C`), 2dp black border, 4dp solid shadow, 16dp rounded corners.
- **Typography:** Black 900 `displaySmall` (`✓ Conversion complete!` / `✓ Saved 42%`).
- **Primary CTA:** Embedded high-contrast button (`Download All as ZIP 📦` / `Save PDF 📄`).

### 1.5. `IndividualOutputCard`
- **Visuals:** Full-width card showcasing rendered bitmap preview, page filename (`page-001.png`), and direct yellow `Download` / `Share` button.

---

## 2. Feature-by-Feature Redesign Matrix

| # | Tool | Accent Color | Web Benchmark Reference | Scope & Key Redesign Changes |
| :- | :--- | :--- | :--- | :--- |
| **1** | **PDF to PNG** | Yellow (`#F5E642`) | [`Web Screenshots/pdf-to-png/`](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/pdf-to-png) | Replace radio buttons with Format Pills (PNG/JPG), Resolution Pills (1x/2x/3x), Page Range Pills, 2-column preview grid, and Stage 3 image cards with individual save/share. |
| **2** | **Images to PDF** | Sky Blue (`#6DC8F3`) | [`Web Screenshots/images-to-pdf/`](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/images-to-pdf) | Add Page Size Pills (A4/Letter/Auto), Orientation Pills (Portrait/Landscape), thumbnail reordering grid with remove badge, and clean PDF save card. |
| **3** | **Merge PDF** | Pink (`#F075C2`) | [`Web Screenshots/merge-pdf/`](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/merge-pdf) | Add multi-file queued cards list, page count inspect chips, drag-to-reorder handles, "Add more files" button, and unified merge progress. |
| **4** | **Split PDF** | Red (`#E8302A`) | [`Web Screenshots/split-pdf/`](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/split-pdf) | Interactive split mode toggle (By Page Range vs Split Every N Pages), numeric stepper buttons (`-` / `+`), start/end thumbnail previews, and ZIP export. |
| **5** | **Compress PDF** | Pink (`#F075C2`) | [`Web Screenshots/compress-pdf/`](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/compress-pdf) | **New Screen:** Recommended vs Maximum compression cards, estimated size tags (`~15% smaller`), savings meter (`✓ Saved 42%`), and single-click compressed save. |
| **6** | **Delete PDF Pages** | Red (`#E8302A`) | [`Web Screenshots/delete-pdf-pages/`](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/delete-pdf-pages) | **New Screen:** 2-column thumbnail grid with interactive tap-to-delete badges, deletion undo bar, remaining pages counter, and pruned export. |
| **7** | **Rearrange PDF** | Mint (`#3DD68C`) | [`Web Screenshots/rearrange-pdf/`](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/rearrange-pdf) | **New Screen:** Smooth drag-and-drop reordering grid, live re-sequenced page badges, reset order action, and reordered document export. |
| **8** | **Protect PDF** | Amber (`#F5A623`) | [`Web Screenshots/protect-pdf/`](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/protect-pdf) | Enhanced password container with Monkey visibility toggles (`🐵` / `🙈`), confirm password validation, first-page padlock preview, and AES-256 export. |
| **9** | **Unlock PDF** | Mint (`#3DD68C`) | [`Web Screenshots/unlock-pdf/`](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/unlock-pdf) | **New Screen:** Encrypted document detection, password prompt with toggle, decryption feedback banner, and unprotected PDF download. |
| **10** | **Add Page Numbers** | Yellow (`#F5E642`) | [`Web Screenshots/add-page-numbers/`](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/add-page-numbers) | 6-position visual quadrant selector (Top/Bottom × Left/Center/Right), starting page number numeric field, format selector (`1`, `Page 1`, `1 of N`), stamped preview. |
| **11** | **Add Watermark** | Sky Blue (`#6DC8F3`) | [`Web Screenshots/add-watermark/`](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/add-watermark) | Dual-mode tabs (Text vs Image watermark), live canvas overlay preview, position matrix, opacity and angle sliders, page scope toggle (Current vs All). |
| **12** | **Sign PDF** | Yellow (`#F5E642`) | [`Web Screenshots/sign-pdf/`](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/sign-pdf) | **New Screen:** Mode pills (Draw, Upload, Type), touch signature canvas, cursive type preview, draggable/resizable signature overlay on PDF page, export. |
| **13** | **PDF to Text** | Sky Blue (`#6DC8F3`) | [`Web Screenshots/pdf-to-text/`](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/pdf-to-text) | Extraction progress bar with page-by-page counter, scrollable preview box with monospaced text, prominent "Copy to Clipboard" and "Save as TXT" CTAs. |
| **14** | **Text to PDF** | Pink (`#F075C2`) | [`Web Screenshots/text-to-pdf/`](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/text-to-pdf) | Direct raw text editor or file picker (.txt/.md/.json), formatting pills (Font size, Margins, Page size), document compilation, and instant PDF download. |
| **15** | **PDF to Word** | Sky Blue (`#6DC8F3`) | [`Web Screenshots/pdf-to-word/`](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/pdf-to-word) | **New Screen:** Document details card, layout reconstruction engine, progress bar, and `.docx` file generator with system file saver contract. |
| **16** | **Edit PDF** | Yellow (`#F5E642`) | [`Web Screenshots/edit-pdf/`](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/edit-pdf) | **New Screen:** Complete mobile editor workspace with mode switcher (Edit Text vs Annotate & Markup), tool pills, page jumper, and export. |

---

## 3. Phased Implementation Roadmap

### Phase 0: Design System & Core Primitives (Week 1)
- **Goal:** Build the missing Neobrutalist Compose components to ensure screen builds are fast and consistent.
- **Deliverables:**
  1. `NeobrutalistPillGroup` and `OptionPill` in `UiComponents.kt`.
  2. `DocumentSummaryHeader` with file metadata formatting.
  3. `PageThumbnailView` using pooled `Bitmap` rendering via `PdfRenderer`.
  4. `ResultsHeroCard` and `IndividualOutputCard` with unified action callbacks.
  5. Haptic feedback and press physics integration.

---

### Phase 1: High-Traffic Conversion Utilities (Week 2)
- **Tools:**
  - `PdfToPngScreen` (Refactor)
  - `ImagesToPdfScreen` (Refactor)
  - `MergePdfScreen` (Refactor)
  - `SplitPdfScreen` (Refactor)
- **Key Enhancements:**
  - Standardize all 4 tools on the strict 3-stage lifecycle (`SingleFileDropZone` → Options/Preview → Results Card).
  - Add thumbnail grids to `PdfToPngScreen` and `SplitPdfScreen`.
  - Add drag-and-drop item reordering to `ImagesToPdfScreen` and `MergePdfScreen`.
  - Support individual page sharing and ZIP export.

---

### Phase 2: Document Security & Stamping Utilities (Week 3)
- **Tools:**
  - `ProtectPdfScreen` (Refactor)
  - `UnlockPdfScreen` (New Implementation)
  - `AddPageNumScreen` (Refactor)
  - `AddWatermarkScreen` (Refactor)
  - `SignPdfScreen` (New Implementation)
- **Key Enhancements:**
  - Implement `UnlockPdfScreen` supporting owner/user password decryption via iTextG.
  - Implement visual position matrix for `AddPageNumScreen` (replacing radio buttons).
  - Implement interactive touch signature capture and typing mode in `SignPdfScreen`.
  - Add live rendered canvas previews to `AddWatermarkScreen`.

---

### Phase 3: Content Extraction & Text Conversion Utilities (Week 4)
- **Tools:**
  - `PdfToTextScreen` (Refactor)
  - `TextToPdfScreen` (Refactor)
  - `PdfToWordScreen` (New Implementation)
- **Key Enhancements:**
  - Implement `PdfToWordScreen` using POI or docx generation pipeline.
  - Polish `PdfToTextScreen` with smooth progress bar and instant clipboard copying.
  - Upgrade `TextToPdfScreen` with formatting pill selectors (A4/Letter, Margins, Font sizes).

---

### Phase 4: Page Management & Document Optimization (Week 5)
- **Tools:**
  - `CompressPdfScreen` (New Implementation)
  - `DeletePdfPagesScreen` (New Implementation)
  - `RearrangePdfScreen` (New Implementation)
- **Key Enhancements:**
  - Implement `CompressPdfScreen` with dual presets (Recommended vs Maximum) and savings meter.
  - Implement `DeletePdfPagesScreen` with interactive deletion flags and undo buffer.
  - Implement `RearrangePdfScreen` with drag-to-reorder grid and immediate page re-indexing.

---

### Phase 5: Advanced Interactive Editor & System-Wide Polish (Week 6)
- **Tools:**
  - `EditPdfScreen` (New Implementation)
  - Full-App Integration, Nav Rail / Dashboard Updates & Regression Testing
- **Key Enhancements:**
  - Implement full-screen `EditPdfScreen` supporting text editing and freehand drawing annotations.
  - Update `HomeScreen` dashboard with all 16 tool cards, search filter, and category tags.
  - Run Roborazzi visual regression tests against the benchmark screenshots in [`Web Screenshots/`](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots).

---

## 4. Mobile Ergonomics & Accessibility Principles

To ensure the native mobile redesign exceeds standard web responsive views:
1. **Thumb-Zone Optimization:** Primary conversion buttons (`Convert`, `Merge`, `Download`) are anchored in the bottom thumb zone with comfortable 16dp margins.
2. **Haptic Confirmation:** Tactile clicks on pills, deletion handles, and download triggers provide physical feedback mimicking hardware buttons.
3. **Storage Access Framework (SAF):** Full integration with Android's system file picker contracts (`OpenDocument`, `CreateDocument`, `OpenMultipleDocuments`) with zero runtime permissions.
4. **Contrast & Sizing:** All text and borders exceed WCAG AAA standards (7:1 contrast ratio against pure black outlines and solid fills).
5. **Offline Guarantee:** Zero network calls, zero analytics beacons, zero cloud dependencies.

---

## 5. Visual Benchmark Reference Index

Every redesign phase must be checked against the reference screenshots captured at `390px × 844px @ 3x DPR`:

- **Master Comparison Directory:** [Web Screenshots/](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots)
- **Individual Tool Folders:**
  - [add-page-numbers/](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/add-page-numbers)
  - [add-watermark/](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/add-watermark)
  - [compress-pdf/](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/compress-pdf)
  - [delete-pdf-pages/](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/delete-pdf-pages)
  - [edit-pdf/](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/edit-pdf)
  - [images-to-pdf/](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/images-to-pdf)
  - [merge-pdf/](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/merge-pdf)
  - [pdf-to-png/](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/pdf-to-png)
  - [pdf-to-text/](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/pdf-to-text)
  - [pdf-to-word/](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/pdf-to-word)
  - [protect-pdf/](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/protect-pdf)
  - [rearrange-pdf/](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/rearrange-pdf)
  - [sign-pdf/](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/sign-pdf)
  - [split-pdf/](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/split-pdf)
  - [text-to-pdf/](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/text-to-pdf)
  - [unlock-pdf/](file:///E:/Antigravity/Projects/Flipp-App/Web%20Screenshots/unlock-pdf)
