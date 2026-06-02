# Codebase Audit and Cleanup Report

## SECTION A — Safe Removals
The following items were confirmed unnecessary, unused and were safely removed to clean up the codebase:
- `firebase-bom` from `app/build.gradle.kts`
- `androidx.room:*` libraries from `app/build.gradle.kts`
- `retrofit2` & `converter-moshi` from `app/build.gradle.kts`
- `moshi-kotlin` & `moshi-kotlin-codegen` from `app/build.gradle.kts`
- `okhttp3` & `logging-interceptor` from `app/build.gradle.kts`
- `secrets-gradle-plugin` (Used only for `.env` AI Studio keys injection)
- `google-devtools-ksp` (Kotlin Symbol Processing, mainly used for Room and Moshi codegen here)
- `.env.example` placeholder file for AI Studio
- The `.env` creation steps and AI Studio URL link from the `README.md` file.
- The `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API` capability from `metadata.json`
- Duplicate resource files such as `.png` launcher icons where `.webp` or `.xml` variants already existed, preventing `duplicate resources` compilation errors.
- Unused `GreetingScreenshotTest.kt` generated from scaffolding causing test build failures.

## SECTION B — Suspicious Code
- `accompanist-permissions`: Commented out in `build.gradle.kts`, could likely be fully removed.
- `camera-core` / `camera-camera2` / `camera-lifecycle` / `camera-view`: Commented out, might indicate a previously abandoned scanner feature.
- `play-services-location`: Commented out, definitely not needed for a local PDF app.
- `coil-compose`: Commented out. The app manually uses `BitmapFactory` and `PdfRenderer` for thumbnails, so Coil isn't needed.

## SECTION C — AI Studio Artifacts
The following Google AI Studio specific generated artifacts were identified and stripped:
- The `secrets-gradle-plugin` to load the `.env` `GEMINI_API_KEY`.
- The `.env.example` boilerplate file.
- The AI Studio app URL and "Run and deploy your AI Studio app" title in `README.md`.
- `metadata.json` containing `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API`.

## SECTION D — Dependency Cleanup
These packages were successfully removed from the `build.gradle.kts` script and `libs.versions.toml`:
- `com.google.firebase:firebase-bom`
- `androidx.room:room-runtime`, `androidx.room:room-ktx`, `androidx.room:room-compiler`
- `com.squareup.retrofit2:retrofit`, `com.squareup.retrofit2:converter-moshi`
- `com.squareup.moshi:moshi-kotlin`, `com.squareup.moshi:moshi-kotlin-codegen`
- `com.squareup.okhttp3:okhttp`, `com.squareup.okhttp3:logging-interceptor`
- `com.google.android.libraries.mapsplatform.secrets-gradle-plugin`
- `com.google.devtools.ksp`

## SECTION E — Architecture Simplification
- The app has been firmly established as a 100% local, offline utility app. By removing Retrofit, OkHttp, Moshi, and Room, the architecture is significantly simplified to just local File I/O and PDF manipulation (`itextg` and Android `PdfRenderer`), reducing the APK size and compilation time.
- Migrating the duplicated mipmap resources simplifies the app structure and ensures a reproducible `assembleDebug` build process.

## SECTION F — Risk Analysis
- **`com.itextpdf:itextg`**: This library is strictly necessary for all PDF manipulation logic (splitting, protecting, stamping) and should not be removed.
- **`android.graphics.pdf.PdfRenderer`**: Built-in Android utility heavily used to generate thumbnails. It is the backbone of the preview UI and cannot be replaced easily without large dependencies.
- **`Robolectric` and `Roborazzi`**: Kept as they are part of the testing architecture (screenshot testing and unit testing). Ensure tests run smoothly on further updates.
