# Edit PDF Canvas and Coordinate Remediation

## Status

The coordinate and export fixes are implemented. Automated geometry, state-retention, and PDF export checks pass. Device-level preview comparison was not performed because no Android device or emulator is available in this environment. The signed release package could not be produced because the configured signing keystore is absent.

## Verified pre-change coordinate flow

- `loadPdfPageBitmap` in `UiComponents.kt` rendered the page to a bitmap at half of `PdfRenderer.Page.width` and `.height`.
- `EditPdfScreen.kt` displayed that bitmap with `ContentScale.Fit` inside a full-width box with a fixed `0.72` aspect ratio. The visible page could therefore be letterboxed inside the touch surface.
- `detectDragGestures` received positions local to the whole box. The code stored those pixel positions in one screen-level stroke list, with no page identifier or transform for the fitted page bounds.
- The preview rendered the page image and text note, but no Compose `Canvas` rendered stored or active freehand strokes.
- Export allocated an overlay at the half-resolution preview bitmap size, painted the raw box positions onto it, and stamped it only on the currently selected PDF page. `stampOverlayOnPdfPage` fitted that bitmap to the PDF page. Coordinates from the full touch box and the smaller, letterboxed page bitmap were therefore not the same coordinate space. Strokes from other pages could also be stamped onto the selected page.
- Text note position and content also lived in screen-level state, so navigation showed the same note on every page and export wrote it only to the selected page.

## Changes made

- Added `PageViewport` to fit the actual rendered page inside its container, map touches to normalized page coordinates, reject touches in letterbox areas, and scale stroke width relative to page width.
- Added a Compose `Canvas` overlay that draws completed strokes and the active stroke using the same fitted page bounds as the PDF image. The overlay clips drawing to the page rectangle.
- Stored stroke and text annotations under their one-based PDF page number. Points, stroke width, and text position/font size are normalized to the displayed page, so a different preview size does not move them.
- Kept annotation state in `EditPdfAnnotationViewModel`, which survives Activity configuration changes without parceling potentially large stroke arrays into Android saved state. Page bitmaps are reloaded when the selected page changes or the Activity is recreated.
- Replaced the Edit PDF raster overlay export with iText vector paths and text written to each annotated page. Export maps top-left normalized coordinates through the PDF crop box and `/Rotate` value, carries color alpha and stroke width, clips to the page box, and leaves existing page content and page dimensions intact. An empty annotation export copies the original PDF bytes unchanged.
- Kept the existing screen layout and Flipp visual styling.

## Files changed

- `app/src/main/java/com/example/EditPdfAnnotationGeometry.kt`
- `app/src/main/java/com/example/EditPdfAnnotationViewModel.kt`
- `app/src/main/java/com/example/PdfUtils.kt`
- `app/src/main/java/com/example/screens/EditPdfScreen.kt`
- `app/src/test/java/com/example/EditPdfAnnotationGeometryTest.kt`
- `app/src/test/java/com/example/PdfUtilsAnnotationsTest.kt`

## Automated verification

The geometry tests cover letterboxing, top-left and bottom-right mapping, out-of-page touches, two preview sizes, normalized stroke thickness, center mapping, and 0/90/180/270-degree PDF rotation. The state test verifies page annotations remain available through the retained ViewModel store. PDF tests use a three-page Letter fixture with 10% grid lines and a center crosshair, rotate page 2 by 90 degrees, export differently colored strokes and a text note to pages 1 and 3, and verify page-specific vector operators, expected coordinates and widths, original text, page dimensions, and rotation. They also verify empty export byte equality and rejection of an invalid page number.

Results from the final source state:

- `:app:testDebugUnitTest` — **BUILD SUCCESSFUL**, 121 tests, 0 failures, 0 errors, 0 skipped.
- Focused Edit PDF tests — **BUILD SUCCESSFUL**.
- `:app:assembleDebug` — **BUILD SUCCESSFUL**.
- `:app:compileReleaseKotlin :app:lintVitalRelease` — **BUILD SUCCESSFUL**.
- `:app:assembleRelease` — **BLOCKED** at `packageRelease` because `my-upload-key.jks` is not present. The final source separately passes release Kotlin compilation and release lint. No signing key was generated or substituted.
- Regression control — the letterbox test failed with the old full-container mapping and passed with the fitted-page transform.

## Visual verification

Not performed. `adb devices -l` returned no attached devices, and the installed Android SDK has no emulator executable. The automated PDF checks verify exported page-space coordinates and vector properties, but do not establish that the live Compose preview screenshot matches a rendered exported page.

Better Design review was unavailable: the required `find-design-system` call returned HTTP 402 because this connection had exhausted its 25 free queries. The UI/UX principle and screen-review calls were therefore unavailable too. The app's existing design was preserved, and this result is not presented as visually reviewed.

## Manual verification procedure

Run this on an Android emulator or device with the final debug APK:

1. Use a three-page PDF with page labels, 10% grid lines, and a center crosshair. The Robolectric fixture setup in `PdfUtilsAnnotationsTest.kt` documents the same page dimensions, grid, and rotation setup; rotate page 2 by 90 degrees.
2. Open Edit PDF. On page 1, draw separate marks near the top-left, center, and bottom-right with different pen colors. Draw on page 3, switch between pages, and confirm each page shows only its own marks.
3. Rotate the device and resize the window. Compare the marks with the grid and confirm their relative positions and widths stay stable. While on page 2, add a mark and confirm it remains on page 2 after navigating away and back.
4. Capture the page preview, export the PDF, then render the saved PDF at 144 DPI with `pdftoppm -f 1 -l 3 -png -r 144 exported.pdf exported-page`.
5. Compare the preview and rendered pages using the grid intersections as reference points. Check the mark centers, page association, stroke colors, and relative thickness. Open the output PDF and confirm original page text and page orientation remain intact.

## Remaining limitations

- Live preview versus exported-page visual comparison remains unverified until the manual procedure can run on an Android device or emulator.
- A signed release package remains unverified until the repository's configured release keystore is supplied through the normal release process.
