---
trigger: always_on
description: "Security and privacy rules for offline, zero-permission local processing in Flipp"
---

# Security & Privacy Guidelines

## 1. Absolute Offline Guarantee
- **Zero Network Permissions:** `AndroidManifest.xml` must never include `android.permission.INTERNET` or network-related permissions.
- **No Network Libraries:** Never introduce HTTP clients, network libraries (OkHttp, Retrofit, Ktor), analytics SDKs, crash reporters, or cloud sync dependencies.
- **Local-Only Processing:** All file conversion, merging, watermarking, and text extraction logic must run entirely in-process on the local Android device.

## 2. Storage & Scoped File Access
- **Storage Access Framework (SAF):** Access files solely via Android's SAF contracts (`OpenDocument`, `OpenMultipleDocuments`, `CreateDocument`).
- **No Broad Storage Permissions:** Never declare `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, or `MANAGE_EXTERNAL_STORAGE`.
- **Ephemeral Processing:** If temporary scratch files are needed in `context.cacheDir` during multi-step conversions, delete them immediately upon completion or error.

## 3. Backup & Data Leakage Prevention
- **System Backups:** Maintain `android:allowBackup="false"` in `AndroidManifest.xml`.
- **Extraction Rules:** Respect `data_extraction_rules.xml` and `backup_rules.xml` to prevent cloud backups or device migration leaks.
- **Log Sanitization:** Never pass sensitive user input, document text, passwords, or absolute file paths into `AppLogger` or system logs.

## 4. Cryptographic & Credential Safety
- **Encryption Strength:** Use 256-bit AES encryption (`PdfWriter.ENCRYPTION_AES_256`) for PDF password protection.
- **No Checked-In Credentials:** Never commit keystores (`*.jks`, `*.keystore`), base64 signing blobs, or credentials to git. Keystores and passwords must only be supplied via CI GitHub Secrets (`KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`) or local environment variables.
