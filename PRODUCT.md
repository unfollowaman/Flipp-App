# PRODUCT.md — Flipp Product Vision & Specifications

## 1. Executive Summary & Vision

**Flipp** is a privacy-first, offline-native Android file converter and document utility. It transforms common document and image manipulation tasks into a rapid, secure, tap-through mobile experience.

### Vision Statement
In an era where everyday document tasks are increasingly routed through cloud-based converters that harvest personal data, log IP addresses, and store sensitive documents on remote servers, Flipp flips the model: **all conversion occurs entirely on the local device, requiring zero accounts, zero network connectivity, and zero data leaves the user's hands.**

---

## 2. Target Audience & Core Personas

1. **Privacy-Conscious Professionals:** Lawyers, accountants, healthcare workers, and executives handling confidential contracts, tax returns, medical records, or proprietary IP that cannot be uploaded to third-party web servers.
2. **Mobile & Remote Workers:** Travelers, field engineers, and commuters frequently working on cellular networks, flights, or off-grid locations without reliable high-speed internet.
3. **Students & Academics:** Users combining lecture slides, formatting papers, numbering report pages, or extracting text from study guides directly on tablets and phones.
4. **General Everyday Users:** Individuals needing to convert receipts to PDF, watermark photos for public marketplaces, or extract text from forms without navigating ad-choked, subscription-gated apps.

---

## 3. Core Problems Solved

| Problem in Current Landscape | Flipp's Local Solution |
| :--- | :--- |
| **Privacy Vulnerability:** Online converters retain copies of sensitive files on cloud servers with vague retention policies. | **Zero Network Activity:** Flipp has no internet permission in its Android manifest. Files never leave device RAM and local storage. |
| **Account Walls & Paywalls:** Basic tools (like PDF merging or password protection) are locked behind subscriptions and email registrations. | **Completely Free & Local:** Open-source utility with zero registration, accounts, subscriptions, or dark patterns. |
| **Connectivity Dependency:** Cloud converters fail when traveling, on planes, or in areas with poor cellular signal. | **Offline-Native Performance:** All rendering and manipulation logic uses Android's native graphics and on-device libraries (`PdfRenderer`, `iTextG`). |
| **Intrusive Permissions:** Conventional utilities demand broad `READ_EXTERNAL_STORAGE` or camera/location permissions. | **Storage Access Framework (SAF):** Flipp requests zero permissions; the user grants scoped access only to the explicit file selected. |

---

## 4. Existing Features & Capabilities

Flipp provides **9 core document and image utilities** accessible from a central Neobrutalist dashboard:

### 1. PDF → PNG (`PdfToPngScreen`)
- **Function:** Converts each page of a PDF document into a high-resolution PNG image.
- **Controls:** Scale multiplier selection (1x Standard, 2x High, 3x Ultra-High).
- **Output:** A single `.zip` archive containing sequentially named image files (`page_1.png`, `page_2.png`, etc.).
- **Performance Optimization:** Direct DEFLATE bypass (`Deflater.NO_COMPRESSION`) to avoid redundant compression cycles.

### 2. Images → PDF (`ImagesToPdfScreen`)
- **Function:** Aggregates one or multiple images into a unified PDF document.
- **Controls:** Page size formatting:
  - `A4`: Automatically scales and centers images to standard ISO A4 margins.
  - `Letter`: Scaled and centered to US Letter proportions.
  - `Auto`: Dynamically sets each PDF page dimension to match the source image aspect ratio.
- **Output:** `.pdf` document.

### 3. Merge PDFs (`MergePdfScreen`)
- **Function:** Concatenates multiple independent PDF documents into a single sequential PDF.
- **Controls:** Add multiple files, re-order selection, inspect file page counts.
- **Output:** `.pdf` document.
- **Memory Optimization:** Reader cache eviction via `freeReader()` after each document import.

### 4. Split PDF (`SplitPdfScreen`)
- **Function:** Extracts a designated subset or range of pages from an existing PDF.
- **Controls:** Interactive start and end page numeric input with bounds validation against total page count.
- **Output:** `.pdf` document containing only selected pages.

### 5. Protect PDF (`ProtectPdfScreen`)
- **Function:** Encrypts an existing PDF document with a user-defined password.
- **Controls:** Password field with visibility toggle and minimum character guidance.
- **Encryption Standard:** 256-bit AES encryption (`PdfWriter.ENCRYPTION_AES_256`) with printing and copying permissions enabled for authorized holders.
- **Output:** Password-protected `.pdf`.

### 6. Add Page Numbers (`AddPageNumScreen`)
- **Function:** Overlays sequential page numbers across all pages of a PDF document.
- **Controls:**
  - 6 Position anchors: Top Left, Top Center, Top Right, Bottom Left, Bottom Center, Bottom Right.
  - Starting page number offset.
- **Output:** `.pdf` document with stamped Helvetica typography.

### 7. Add Watermark (`AddWatermarkScreen`)
- **Function:** Stamps custom text or image watermarks onto images.
- **Controls:**
  - Watermark mode: Text or Image.
  - Text: Content string, hex color selection (Black, White, Red, Blue).
  - Geometry: 7 Position anchors (including Center), rotation angle (-180° to 180°), opacity slider (0.0 to 1.0), and size multiplier.
- **Performance Optimization:** Single-pass `Matrix` canvas transformation with `inSampleSize` decode bounds downsampling.
- **Output:** Processed image (PNG or JPEG based on input).

### 8. Text → PDF (`TextToPdfScreen`)
- **Function:** Formats raw text into a cleanly styled, printable PDF document.
- **Controls:** Page size (A4, Letter), margin presets (Small, Normal, Large), font size slider (8pt to 24pt), text alignment (Left, Center, Right, Justified).
- **Output:** Standard `.pdf` document.

### 9. PDF → Text (`PdfToTextScreen`)
- **Function:** Extracts selectable embedded text from all pages of a PDF document.
- **Controls:** Page progress indicator, live text preview box, "Copy to Clipboard" action, and "Save as TXT" action.
- **Output:** System clipboard content or `.txt` plain-text file.

---

## 5. User Journeys & Workflow Lifecycle

All 9 tools adhere to a predictable, 4-stage workflow designed for clarity and confidence:

```
[ Stage 1: File Ingestion ]
           │
           ▼
[ Stage 2: Configuration & Preview ]
           │
           ▼
[ Stage 3: Local Processing (Progress Bar) ]
           │
           ▼
[ Stage 4: Result Export & Save via SAF ]
```

1. **Stage 1 — Ingestion:** The user taps the tool card from the Home dashboard. A brutalist `DropZone` prompts the user to select input file(s) via Android's native system picker.
2. **Stage 2 — Configuration & Preview:** The user configures tool-specific parameters (resolution, password, margin, position). Where applicable, asynchronous thumbnail generation displays the first few pages of the document.
3. **Stage 3 — Local Processing:** Upon tapping the action button, the app transitions to a non-blocking coroutine. A `StageProgressBar` reflects real-time progress percentages as pages or items are transformed.
4. **Stage 4 — Result Export:** When complete, the app invokes `ActivityResultContracts.CreateDocument` to let the user choose the exact local directory and file name. A confirmation card offers quick access to save again or return to the dashboard.

---

## 6. Product Limitations & Technical Boundaries

- **No Optical Character Recognition (OCR):** The `PDF → Text` utility extracts embedded digital text streams. Scanned, photographed, or purely rasterized PDFs do not contain selectable text and require OCR, which is currently out of scope.
- **Device Hardware Dependency:** Because processing is 100% on-device, conversion speed and maximum file handling capacity are directly proportional to the device's CPU and RAM. Processing 500MB+ PDFs or dozens of 50MP images may trigger memory warnings on low-spec hardware.
- **No Cloud Synchronization:** Flipp purposefully does not provide cloud backup, user document histories, or cross-device syncing to ensure zero external data transmission.

---

## 7. Future Strategic Opportunities

- **On-Device OCR Integration:** Integrate Google ML Kit Text Recognition (offline bundle) to allow text extraction from scanned documents and images.
- **PDF Page Reordering & Rotation:** Enable a visual grid view where users can drag and drop pages to re-order, rotate individual pages, or delete redundant pages prior to export.
- **PDF Compression Utility:** Offer image quality reduction and stream optimization within PDFs to shrink document file size for email attachments.
- **Tool Chaining:** Enable piping the output of one tool directly into another (e.g., Image → PDF → Protect PDF) without writing an intermediate file to disk.
- **System Dark Theme:** Expand the Neobrutalist design system to incorporate a high-contrast dark theme mode.
