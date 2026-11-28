# Flipp Android — Google Play Store Assets Remediation

**Audit item:** `FLIPP-P0-003`

**Checked:** November 28, 2026

**Scope:** Store assets and listing copy only. Nothing was uploaded or published; no app source, package ID, version, signing setup, or dependency was changed.

## Status

| Item | Status | Result |
|---|---|---|
| Play Store icon | **Complete** | 512 × 512 PNG, 8-bit RGBA / 32-bit color type. Existing RGB artwork is pixel-identical; the added alpha channel is fully opaque. |
| Feature graphic | **Complete** | 1024 × 500 PNG, 8-bit RGB / 24-bit, no alpha. Editable SVG source included. |
| Android phone screenshots | **Blocked** | No connected Android device or emulator/AVD is available in this environment. No substitute screenshots were created. See the capture plan below. |
| Short description | **Complete** | 65 characters, within the 80-character limit. |
| Full description | **Complete** | 1,089 characters, within the 4,000-character limit. |
| Privacy policy | **Partially complete** | Source-checked HTML is ready to host. No public HTTPS URL exists yet, and the app does not currently link to a hosted policy. See the privacy findings and manual actions below. |

**Completion gate:** `FLIPP-P0-003` is not fully resolved. Genuine screenshots and a publicly hosted HTTPS privacy policy with an in-app link are still required.

## Files prepared

- `store-assets/play-store-icon-512.png` — 512 × 512, PNG color type 6 (RGBA), 8 bits/channel. The RGB pixels match the original icon exactly; alpha values are all 255.
- `store-assets/feature-graphic-1024x500.png` — 1024 × 500, PNG color type 2 (RGB), 8 bits/channel, opaque as required by current Play guidance. It uses Flipp's packaged mascot and Bricolage Grotesque font, with the existing yellow, cream, mint, and black palette. The copy sits beside the illustration; the paper motif is a simple illustration, not app UI.
- `store-assets/feature-graphic-1024x500.svg` — self-contained editable source with the mascot and packaged font embedded.
- `store-assets/short-description.txt`
- `store-assets/full-description.txt`
- `store-assets/privacy-policy.html` — static HTML with inline styling and no scripts, external fonts, image requests, or hosted URL claim.
- `release-audit/STORE_ASSETS_REMEDIATION.md` — this status report and remaining-work plan.

The graphic was inspected locally at full size. Better Design review calls failed with HTTP 402 quota exhaustion, so no Better Design review was completed or claimed.
The existing Android launcher mascot and Play Store icon were also inspected. The icon itself was not redesigned or resampled; its RGB pixels remain unchanged. The original mascot was scaled for the feature graphic.

## Store descriptions

**Short description (65/80):**

> Convert, edit, organize, and protect PDFs on your Android device.

**Full description (1,089/4,000):**

> Flipp brings 16 tools for PDFs and images to one Android app. Select a file, choose a tool, and save the result to a location you choose.
>
> PDF AND IMAGE TOOLS
> • Convert PDF pages to PNG images in a ZIP archive.
> • Create PDFs from images or text.
> • Merge PDFs, extract a page range, rearrange pages, or delete pages.
> • Add page numbers or edit PDF pages with text and drawings.
> • Sign PDFs by drawing, typing, or placing a signature.
> • Protect PDFs with a password or unlock a PDF when you know its password.
> • Convert PDFs to text or Word (.docx), and compress PDFs.
> • Add text or image watermarks to images.
>
> Flipp processes selected files on your Android device and declares no INTERNET permission. Choose inputs and output destinations with Android's document picker. If you select a cloud or third-party file provider, that provider may handle transfers under its own privacy policy. Some tools use temporary files in Flipp's private cache while processing.
>
> PDF-to-text extracts text already embedded in a document. Scanned pages that contain only images may not have text to extract.

The feature list was checked against the 16 tool cards and routes in `app/src/main/java/com/example/MainActivity.kt`, plus their Android screen implementations. The copy avoids claiming OCR, guaranteed compression, or that every provider-backed file transfer is offline.

## Privacy-policy findings

The policy in `store-assets/privacy-policy.html` is based on the Android source and configuration:

- `app/src/main/AndroidManifest.xml` contains no `<uses-permission>` entries. The merged release manifest contains one app-scoped signature permission (`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`) contributed by AndroidX Core, but no `INTERNET` or broad storage permission. It declares a non-exported `FileProvider` scoped to the app cache.
- File selection and export use Android's Storage Access Framework contracts (`OpenDocument`, `OpenMultipleDocuments`, and `CreateDocument`). A user-selected third-party or cloud document provider controls any transfer or storage it performs.
- The tools process selected files locally. Some operations create output and intermediate files in `context.cacheDir`; screens attempt cleanup on workflow/reset paths. `clearStaleScratchFiles` on app launch deletes only selected known names, so cleanup is best-effort and an interrupted operation can leave cache files.
- `AppLogger` writes diagnostic messages and exceptions to Android's system log. The offline-resolved `debugRuntimeClasspath` contains AndroidX/Compose, Kotlin/coroutines, iTextG, and SpongyCastle; no analytics, advertising, crash-reporting, or network-client SDK was found.
- PDF-to-text copies content to the Android clipboard only when the user taps the copy action.
- `android:allowBackup="false"` is set, and both backup and data-extraction rules exclude app data.

**Existing in-app disclosure mismatch:** `MainActivity.kt` currently says the app does not store, copy, or cache documents after conversion. That statement conflicts with the cache files created by PDF/image conversion and other tool screens. This task leaves app source unchanged; correct that in-app text before release, alongside adding a link to the hosted policy.

## Local verification (November 28, 2026)

- `testDebugUnitTest --offline` — **passed**: 133 tests across 22 JUnit reports; 0 failures, errors, or skipped tests.
- `:app:assembleDebugAndroidTest --offline` — **passed**: instrumented test sources compiled and the test APK packaged; the tests were not run on a device.
- `lintDebug --offline` — **passed with warnings**: 29 warnings, 0 errors. Findings are 7 `DefaultLocale`, 1 `RedundantLabel`, 2 `GradleDependency`, 1 `ObsoleteSdkInt`, 7 `UnusedResources`, 2 `MonochromeLauncherIcon`, and 9 `UseKtx`. Android source was not changed for this store-assets task.
- `:app:dependencies --configuration debugRuntimeClasspath --offline` — **passed**; the resolved runtime graph lists AndroidX/Compose, Kotlin/coroutines, iTextG, and SpongyCastle, with no analytics, advertising, crash-reporting, or network-client SDK found.
- Asset integrity checks — **passed**: the icon is valid 512 × 512 RGBA8 with fully opaque alpha and RGB pixels identical to the prior tracked icon; the feature graphic is valid opaque 1024 × 500 RGB8; SVG and privacy HTML parse; descriptions are 65 and 1,089 characters; 16 dashboard tools are present in source.
- Merged manifests — **passed**: debug and release each contain only the app-scoped signature permission `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`; neither contains `INTERNET` or broad storage permissions.
- `git diff --check` — **passed**.
- `connectedAndroidTest --offline` — **not executed**: Gradle could not resolve the uncached `com.android.tools.utp:android-test-plugin-host-additional-test-output:32.1.1` artifact in offline mode. ADB reports no attached devices; the emulator executable and AVD directory are absent.

The policy uses the app name Flipp and directs privacy questions to the developer contact shown on the Google Play listing. No developer email, hosting service, or public URL was invented.

## Genuine screenshot capture plan

No phone screenshot files are present. The SDK `adb.exe` exists, but its server failed to start; no emulator executable, AVD, or connected device was found. Capture these from the real Flipp app once a device/emulator is available:

1. **Home dashboard** — show the hero and the start of the 16-tool grid.
2. **Merge PDFs** — use two generic sample documents, such as `Sample-overview.pdf` and `Sample-notes.pdf`, with the selected files and available ordering/preview visible.
3. **Edit PDF** — open a one-page synthetic sample document and show an actual annotation on the page; dismiss the keyboard and picker.
4. **PDF to Text** — show extracted selectable text from the same sample document and the real copy/save actions; dismiss keyboard and transient toast.

Use generic, locally generated documents with no personal information. Install the actual app build on an Android emulator or physical phone configured for portrait **1080 × 1920 (9:16)**. Save untouched device captures as `store-assets/phone-screenshots/01-home.png` through `04-pdf-to-text.png`, for example with `adb exec-out screencap -p` while each app screen is foregrounded. Do not add device frames, overlay marketing text, substitute website images, or fabricate/edit the app UI. Before upload, confirm each screenshot is a genuine app screen, is PNG or JPEG, has sides between 320 and 3,840 pixels, and is no larger than 8 MB. Google Play currently accepts 2–8 phone screenshots; this plan prepares four.

## Remaining manual actions

1. Publish `store-assets/privacy-policy.html` as a normal, non-editable webpage on a public HTTPS host. Verify the page is reachable worldwide without login or geofencing and has a valid TLS certificate. Then record the actual URL; none is available in this workspace.
2. Ensure the Play listing's developer contact details are accurate, then add the hosted URL to the Play listing and to the app's Privacy screen. Update that screen's cache disclosure and build a new app release containing the in-app link.
3. Capture and verify the four real phone screenshots above on an emulator or device.
4. Upload the finished assets and enter the prepared descriptions only when you choose to work in Play Console. No upload or publication was performed here.

Current official Play guidance used for these checks: [store listing text limits](https://support.google.com/googleplay/android-developer/answer/9859152), [preview asset and screenshot requirements](https://support.google.com/googleplay/android-developer/answer/9866151), and [privacy-policy hosting and linking requirements](https://support.google.com/googleplay/android-developer/answer/9859455).
