# Security and Integrity Audit Report

This repository was reviewed as a privacy-first, offline Android PDF/image utility. The cleanup in this audit removed only repository artifacts that were not referenced by Gradle, Android resources, Kotlin source, tests, or documentation after the documentation was updated.

## Confirmed Removals

- Removed the checked-in generated debug APK (`.build-outputs/app-debug.apk`). APK outputs are build artifacts and should be recreated from source.
- Removed the checked-in Base64 debug keystore (`debug.keystore.base64`). Debug signing material should not be stored as repository content.
- Removed tracked `local.properties` from source control. It is environment-specific and already ignored by `.gitignore`.
- Removed AI Studio/project-generator metadata artifacts (`metadata.json`, `assets/aistudio/gitignore`) that were not part of the Android build.
- Removed the duplicate `download` ignore-template file.
- Removed the empty `app/src/test/screenshots` placeholder file.
- Removed inactive/commented dependency declarations and their unused version-catalog aliases for camera, location, datastore, navigation, Coil, Accompanist, and extended material icons.

## Findings Preserved for Human Review

- `com.itextpdf:itextg:5.5.10` is required for core PDF operations and was not removed. It should be reviewed for replacement or mitigation because the Android-specific iTextG artifact is old, while related iText 5 security advisories recommend newer iText 5.x lines for non-Android artifacts.
- The manifest keeps `android:allowBackup="true"`. The current app does not define persistent user-document storage, but a privacy-first release should explicitly review Android backup/data-extraction policy before production distribution.
- The Gradle wrapper JAR is not tracked, so `./gradlew` cannot currently run from a clean checkout. That is a build reproducibility issue and should be addressed separately.
