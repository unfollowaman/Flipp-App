# Flipp Android — Task 1: Investigation & Remediation Report
**Subject:** PDF Encryption/Decryption Fatal Crash Resolution (`java.lang.NoClassDefFoundError: org/spongycastle/asn1/ASN1Encodable` / `ASN1OctetString`)  
**Component:** `com.example.PdfUtils`, `ProtectPdfScreen`, `UnlockPdfScreen`, R8 Configuration, Dependency Catalog  
**Status:** Resolved & Verified  

---

## 1. Verified Root Cause

### 1.1 The Failure Mechanism
When invoking `PdfUtils.protectPdf` (or attempting to read an encrypted PDF via `PdfReader`), the application suffered an immediate fatal crash:
```text
java.lang.NoClassDefFoundError: org/spongycastle/asn1/ASN1OctetString
    at com.itextpdf.text.pdf.PdfEncryption.<init>(PdfEncryption.java:147)
    at com.itextpdf.text.pdf.PdfWriter.setEncryption(PdfWriter.java:2131)
    at com.itextpdf.text.pdf.PdfStamper.setEncryption(PdfStamper.java:312)
    at com.example.PdfUtils.protectPdf(PdfUtils.kt:286)
```

Bytecode decompilation of `com.itextpdf.text.pdf.PdfEncryption` from `itextg-5.5.10.jar` reveals the exact trigger at line 147 of the `PdfEncryption` constructor:
```java
public PdfEncryption() {
    ...
    publicKeyHandler = new PdfPublicKeySecurityHandler(); // Line 147
}
```
Regardless of whether a user specifies standard password-based encryption (AES-128 or AES-256) or certificate encryption, `PdfEncryption.<init>()` **unconditionally** instantiates `PdfPublicKeySecurityHandler`. 

In turn, `PdfPublicKeySecurityHandler` directly imports and references ASN.1 structures and CMS classes from the SpongyCastle packages (`org.spongycastle.asn1.*`, `org.spongycastle.asn1.cms.*`, `org.spongycastle.asn1.x509.*`). When the JVM classloader attempts to link `PdfPublicKeySecurityHandler`, it fails to find `org.spongycastle.asn1.ASN1OctetString` (and `ASN1Encodable`), throwing `ClassNotFoundException`, which surfaces as a fatal `java.lang.NoClassDefFoundError`.

### 1.2 Why the Crash Bypassed UI Error Handling
In `ProtectPdfScreen.kt` and `UnlockPdfScreen.kt`, background coroutines wrapped operations in:
```kotlin
try {
    PdfUtils.protectPdf(...)
} catch (e: Exception) {
    // Show error toast
}
```
Because `java.lang.NoClassDefFoundError` inherits from `java.lang.LinkageError` $\rightarrow$ `java.lang.Error` $\rightarrow$ `java.lang.Throwable` (not `java.lang.Exception`), the catch block was bypassed completely. The uncaught error propagated up the coroutine exception hierarchy and terminated the Android process.

### 1.3 Why the Build Passed While Runtime Failed
Inspection of `app/build.gradle.kts` and `itextg-5.5.10.pom` revealed:
1. `itextg:5.5.10` POM defines `com.madgag:scprov-jdk15on` and `scpkix-jdk15on` with `<optional>true</optional>`. Maven and Gradle do not pull optional dependencies transitively.
2. In `app/proguard-rules.pro`, line 20 suppressed R8 missing-class verification warnings using:
   ```proguard
   -dontwarn org.spongycastle.**
   ```
This allowed R8 to assemble the release APK without errors despite critical runtime classes missing from the compile and runtime classpaths.

---

## 2. Affected Code Paths

Every operation invoking `PdfEncryption` or reading an encrypted PDF stream was affected:
1. **`PdfUtils.protectPdf`**: Direct invocation of `stamper.setEncryption(...)` with `PdfWriter.ENCRYPTION_AES_256`. Crashes 100% of the time on first invocation.
2. **`PdfUtils.unlockPdf`**: Calling `PdfReader(inputStream, password.toByteArray())` on an encrypted PDF instantiates `PdfEncryption` to parse the `/Encrypt` trailer dictionary and derive the AES key. Crashes 100% of the time.
3. **`PdfUtils.isPdfEncrypted`**: When fallback stream inspection (`PdfReader(stream)`) executes for a document whose `ParcelFileDescriptor` cannot be opened or whose header requires fallback, `PdfReader` instantiates `PdfEncryption`, crashing with `NoClassDefFoundError` instead of gracefully returning `true`.
4. **All PDF processing features processing encrypted PDFs**:
   - `mergePdfs`, `splitPdf`, `addPageNumbers`, `compressPdf`, `signPdf`, `watermarkPdf`, `extractPages`, `reorderPdfPages`, and `extractTextFromPdf`.
   - If a user selected a password-protected PDF in any of these screens, `PdfReader` crashed the entire app rather than throwing a catchable `BadPasswordException`.
5. **UI Screens**:
   - `ProtectPdfScreen.kt`: Stage 3 processing crash.
   - `UnlockPdfScreen.kt`: Stage 2 unlock crash.

---

## 3. Dependency & License Findings

### 3.1 License Breakdown
| Library Artifact | Declared License | License Type | Distribution Implications for Flipp |
| :--- | :--- | :--- | :--- |
| `com.itextpdf:itextg:5.5.10` | GNU AGPLv3 | Strong Copyleft | Distributing compiled APK/AAB binaries triggers Section 6 source disclosure requirements. If Flipp is distributed as open-source, AGPLv3 is satisfied; for closed-source commercial distribution, a commercial license from Apryse is required. |
| `com.madgag:scprov-jdk15on:1.47.0.2` | Bouncy Castle Licence | Permissive (MIT-style) | No copyleft obligations. No source disclosure required. Fully compatible with AGPLv3, Apache 2.0, and proprietary licensing. |
| `com.madgag:scpkix-jdk15on:1.47.0.2` | Bouncy Castle Licence | Permissive (MIT-style) | Same permissive terms as `scprov`. Compatible with all distribution models. |
| `com.tom-roush:pdfbox-android` (Evaluated) | Apache License 2.0 | Permissive | Notice requirement; no copyleft. |

### 3.2 License Assessment
- Adding `scprov-jdk15on` and `scpkix-jdk15on` **does not introduce any new copyleft obligations or licensing risks**. The Bouncy Castle license is an extremely permissive BSD/MIT variant.
- The AGPLv3 obligation already exists in Flipp because `itextg:5.5.10` is used across 11 other features in the app. Resolving the missing SpongyCastle classes does not alter the preexisting legal status of `itextg`.

---

## 4. PDF-Engine Options Evaluated

### Option 1: Repair Existing iText Engine with Official SpongyCastle Dependencies
- **Runtime Compatibility**: Android API 26 (`minSdk`) through API 36 (`targetSdk`). Pure Java bytecode compatible with Java 11/17 runtimes and Android ART.
- **AES Encryption / Security Revisions**: Full support for Standard Security Handler Revision 2 (40-bit RC4), Revision 3 (128-bit RC4/AES), Revision 4 (AES-128), and Revision 5/6 (AES-256 conforming to ISO 32000-1 / Extension Level 3). Also satisfies `PdfPublicKeySecurityHandler`.
- **Password-Protected PDF Compatibility**: Fully supports opening, validating, decrypting, and re-encrypting password-protected PDFs. Throws standard catchable `BadPasswordException`.
- **Existing Feature Compatibility**: 100% compatible. Flipp already implements 11 features on `itextg`. No code rewrites or regression risks to existing utilities.
- **Maintenance & Security**: SpongyCastle 1.47.0.2 is unmaintained upstream. However, Flipp is a **100% offline application** with zero network permissions (`android.permission.INTERNET` is strictly absent). Known remote vulnerabilities in Bouncy Castle TLS/network parsing cannot be triggered.
- **Binary Footprint & R8**: With R8 shrinking and tree shaking enabled, unused cryptographic algorithms are stripped, resulting in minimal DEX expansion. Allows complete removal of `-dontwarn org.spongycastle.**`.
- **Complexity**: Low risk, targeted, verifiable.

### Option 2: Android Native PDF APIs (`android.graphics.pdf`)
- **Runtime Compatibility**: Evaluated against Flipp's `minSdk = 26`.
- **Capabilities**:
  - `PdfRenderer`: From API 26 to API 34, `PdfRenderer` does not accept passwords; opening any encrypted PDF unconditionally throws `SecurityException`. Password support was only introduced in Android 15 (API 35+).
  - `PdfDocument`: Native `PdfDocument` provides canvas-to-PDF rendering only; it contains **zero** encryption, decryption, or security APIs.
- **Verdict**: Technically impossible as a replacement for Protect PDF or Unlock PDF under `minSdk = 26`.

### Option 3: Apache PDFBox-Android (`com.tom-roush:pdfbox-android`)
- **Runtime Compatibility**: Android API 19+.
- **Capabilities**: Full AES-128 and AES-256 support via `StandardProtectionPolicy`.
- **Evaluation**:
  - *Partial Replacement (Protect/Unlock only)*: Results in two distinct PDF engines in the application (`itextg` 2.2 MB + `pdfbox-android` 2.5 MB), causing significant APK bloat. Furthermore, opening encrypted PDFs in any of the other 11 features would still trigger the `itextg` crash unless SpongyCastle is added anyway.
  - *Full Engine Migration*: Would require rewriting all 11 document conversion, merge, split, watermark, page-numbering, and compression utilities (~900 lines of complex PDF logic in `PdfUtils.kt`), violating working rule 5 (*"Do not perform a broad PDF-engine migration without establishing necessity"*).
- **Verdict**: Inappropriate for a targeted bug fix; viable only if the project leadership mandates a future architectural migration away from AGPL.

---

## 5. Chosen Approach & Technical Rationale

**Decision:** Implement the targeted dependency fix using `com.madgag:scprov-jdk15on:1.47.0.2` and `com.madgag:scpkix-jdk15on:1.47.0.2` alongside defensive updates in `PdfUtils.kt` and the removal of the suppressive ProGuard rule.

**Rationale:**
1. Directly addresses the verified root cause without introducing redundant libraries or architectural churn.
2. Restores full functionality to Protect PDF, Unlock PDF, and encrypted PDF reading across all tools.
3. Removes the build-time warning suppression (`-dontwarn org.spongycastle.**`), restoring R8 compile-time verification integrity.
4. Preserves 100% offline privacy and existing application architecture.

---

## 6. Files Changed

1. **`gradle/libs.versions.toml`**:
   - Added version `spongycastle = "1.47.0.2"`.
   - Declared `spongycastle-prov = { group = "com.madgag", name = "scprov-jdk15on", version.ref = "spongycastle" }`.
   - Declared `spongycastle-pkix = { group = "com.madgag", name = "scpkix-jdk15on", version.ref = "spongycastle" }`.
2. **`app/build.gradle.kts`**:
   - Added `implementation(libs.spongycastle.prov)` and `implementation(libs.spongycastle.pkix)`.
3. **`app/proguard-rules.pro`**:
   - Removed `-dontwarn org.spongycastle.**`.
4. **`app/src/main/java/com/example/PdfUtils.kt`**:
   - `protectPdf`: Enforced `require(password.isNotEmpty())`, wrapped reader in `try-finally` to ensure proper resource closure, switched password parameter to standard Kotlin `String`, and used `password.toByteArray()`.
   - `unlockPdf`: Wrapped `reader` in `try-finally` to guarantee resource disposal on exception.
   - `isPdfEncrypted`: Wrapped `reader` in `try-finally` and broadened error handling to catch `Throwable` so unexpected linkage errors cannot crash file inspection.
5. **`app/src/main/java/com/example/screens/ProtectPdfScreen.kt`**:
   - Cleaned up redundant `java.lang.String(password)` wrapping to pass `password` directly.
6. **`app/src/test/java/com/example/PdfUtilsProtectTest.kt`**:
   - Replaced fragile `NoClassDefFoundError` catch with comprehensive tests for AES-256 encryption, password rejection, corrupt inputs, empty passwords, and page content integrity.
7. **`app/src/test/java/com/example/PdfUtilsUnlockTest.kt`** (New):
   - Created full test suite for unlocking password-protected PDFs, verifying decryption, page counts, content preservation, and error handling.

---

## 7. Tests Executed & Actual Results

### 7.1 Automated Unit & Integration Tests (Robolectric SDK 34)
Command executed:
```bash
./gradlew testDebugUnitTest --tests "com.example.PdfUtilsProtectTest" --tests "com.example.PdfUtilsUnlockTest"
```
**Result:** `BUILD SUCCESSFUL` (14/14 tests passed, 0 failures, 0 errors).

| Test Suite | Test Case | Purpose & Requirement Coverage | Status |
| :--- | :--- | :--- | :---: |
| `PdfUtilsProtectTest` | `test1_protectOrdinaryPdfWithPassword_createsEncryptedPdfWithExpectedPages` | Req 1 & 8: Encrypts 2-page PDF with AES-256 password; verifies unauthenticated opening throws `BadPasswordException`; verifies authenticated opening matches expected page count (2) and text content. | **PASSED** |
| `PdfUtilsProtectTest` | `test2_openProtectedPdfWithCorrectPassword` | Req 2: Validates opening protected PDF with correct password. | **PASSED** |
| `PdfUtilsProtectTest` | `test3_rejectIncorrectPasswordGracefully` | Req 3: Verifies opening with no password or incorrect password throws `BadPasswordException`. | **PASSED** |
| `PdfUtilsProtectTest` | `test5_handleInvalidOrCorruptedPdf` | Req 5: Verifies corrupted/malformed input stream throws graceful `Exception`. | **PASSED** |
| `PdfUtilsProtectTest` | `test6_handleAlreadyEncryptedPdf` | Req 6: Verifies encrypting an already-encrypted PDF throws `BadPasswordException` without crashing. | **PASSED** |
| `PdfUtilsProtectTest` | `test7_handleUnsupportedOrMalformedEncryptionGracefully` | Req 7: Verifies `isPdfEncrypted` safely handles corrupt/unsupported formats without throwing unhandled JVM errors. | **PASSED** |
| `PdfUtilsProtectTest` | `testProtectPdf_emptyPassword_throwsIllegalArgumentException` | Validates input validation rejecting blank passwords. | **PASSED** |
| `PdfUtilsProtectTest` | `testProtectPdf_nonExistentUri_throwsException` | Validates handling of non-existent file URIs. | **PASSED** |
| `PdfUtilsProtectTest` | `testIsPdfEncrypted_detectsEncryptedAndUnencryptedPdfsCorrectly` | Verifies detection accuracy on plain vs AES-256 encrypted PDFs. | **PASSED** |
| `PdfUtilsUnlockTest` | `test4_unlockPasswordProtectedPdf_producesDecryptedPdfWithExpectedPages` | Req 4 & 8: Decrypts AES-256 protected PDF with password; verifies resulting PDF has `isEncrypted = false`, opens without credentials, and preserves 2 pages and text. | **PASSED** |
| `PdfUtilsUnlockTest` | `test_unlockPdf_withWrongPassword_throwsBadPasswordException` | Req 3: Verifies decryption attempt with wrong password throws `BadPasswordException`. | **PASSED** |
| `PdfUtilsUnlockTest` | `test_unlockPdf_corruptedFile_throwsException` | Req 5: Verifies unlocking corrupt file throws `Exception`. | **PASSED** |
| `PdfUtilsUnlockTest` | `test_unlockPdf_unencryptedPdf_handlesGracefully` | Verifies unlocking an already unencrypted PDF passes through cleanly. | **PASSED** |
| `PdfUtilsUnlockTest` | `test_unlockPdf_nonExistentUri_throwsException` | Validates non-existent URI handling for unlock. | **PASSED** |

### 7.2 Full Repository Regression Test Suite
Command executed:
```bash
./gradlew testDebugUnitTest
```
**Result:** `BUILD SUCCESSFUL` (All existing repository unit and Compose tests passed without regression).

### 7.3 Release & R8 Verification Build
Command executed:
```bash
./gradlew :app:minifyReleaseWithR8
```
**Result:** `BUILD SUCCESSFUL` (28 tasks executed/cached, R8 code shrinking and optimization completed with zero missing class warnings after removing `-dontwarn org.spongycastle.**`).

---

## 8. Remaining Limitations & Risks

1. **Upstream Maintenance of SpongyCastle**: `scprov-jdk15on:1.47.0.2` has had no upstream releases since 2014. While secure in Flipp's 100% offline, zero-network environment, static vulnerability scanners (e.g. Snyk, Dependabot) may flag CVEs associated with Bouncy Castle 1.47 (such as CVE-2016-1000338). These CVEs relate to TLS servers and remote ASN.1 certificate parsing, neither of which is present in Flipp.
2. **AGPL-3.0 Licensing for Commercial Release**: `itextg:5.5.10` remains under the AGPLv3 license. If Flipp is published as an open-source application on GitHub with AGPLv3, distribution complies with licensing terms. If Flipp is intended as a closed-source proprietary app on Google Play, a long-term roadmap to migrate the entire app to an Apache 2.0 engine (e.g. PDFBox-Android) must be scheduled.
