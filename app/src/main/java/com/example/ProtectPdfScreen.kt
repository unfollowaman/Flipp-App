package com.example

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlackColor
import com.example.ui.theme.FlippYellow
import com.example.ui.theme.MintColor
import com.example.ui.theme.RedColor
import com.example.ui.theme.YellowColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun ProtectPdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1: Idle, 2: File Selected & Settings, 3: Processing, 4: Completed, 5: Error
    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSizeStr by remember { mutableStateOf<String?>(null) }
    var totalPages by rememberSaveable { mutableIntStateOf(1) }
    var previewThumbnail by remember { mutableStateOf<Bitmap?>(null) }

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var progressVal by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf("") }
    var tempPdfFile by remember { mutableStateOf<File?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            tempPdfFile?.delete()
            previewThumbnail?.recycle()
        }
    }

    val pdfSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            if (uri != null && tempPdfFile != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            tempPdfFile!!.inputStream().use { input ->
                                input.copyTo(out)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Encrypted PDF protected successfully!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    )

    FeaturePageScaffold(
        title = "Protect PDF",
        desc = "Password-encrypt a PDF file. Encrypts documents to prevent unauthorized access.",
        badgeLabel = "PROTECT",
        onBack = onBack,
        badgeColor = YellowColor
    ) {
        when (stage) {
            1 -> {
                // State 1: Idle
                SingleFileDropZone(
                    mimeTypes = arrayOf("application/pdf"),
                    prompt = "AES password-encrypt a PDF entirely on-device.",
                    badgeColor = YellowColor,
                    onFileSelected = { uri ->
                        selectedPdfUri = uri
                        fileName = FileUtils.getFileName(context, uri)
                        fileSizeStr = FileUtils.getFileSizeString(context, uri)

                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val pages = PdfUtils.getPdfPageCount(context, uri)
                                val thumbs = loadPdfThumbnails(context, uri, maxPages = 1)
                                withContext(Dispatchers.Main) {
                                    totalPages = pages
                                    previewThumbnail = thumbs.firstOrNull()
                                    stage = 2
                                }
                            } catch (_: Exception) {
                                withContext(Dispatchers.Main) {
                                    stage = 2
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                FeatureHowItWorksCard(
                    step1 = "Select PDF document to encrypt",
                    step2 = "Enter secure encryption password (256-bit AES)",
                    step3 = "Download protected PDF document"
                )
            }

            2 -> {
                // State 2 & 3: File Selected & Settings
                SelectedFileCard(
                    fileName = fileName.ifEmpty { "document.pdf" },
                    fileSizeStr = fileSizeStr,
                    pageCount = totalPages,
                    fileTypeBadge = "PDF",
                    badgeColor = YellowColor,
                    previewBitmap = previewThumbnail,
                    onChangeFile = {
                        selectedPdfUri = null
                        previewThumbnail = null
                        stage = 1
                    }
                )

                CollapsibleSettingsCard(title = "Configure Password Settings") {
                    Text(
                        text = "Set Document Password (AES-256):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Enter encryption password", color = Color.Gray) },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Encryption password" },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BlackColor,
                            unfocusedBorderColor = BlackColor
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Confirm Password:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = { Text("Re-enter password to confirm", color = Color.Gray) },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Confirm encryption password" },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BlackColor,
                            unfocusedBorderColor = BlackColor
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "🔒 Security: 256-bit AES encryption block. This password cannot be retrieved by anyone if lost. Keep it safe.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF666666),
                        lineHeight = 16.sp
                    )
                }

                BrutalistButton(
                    text = "Convert All Pages →",
                    onClick = {
                        if (password.trim().isEmpty()) {
                            Toast.makeText(context, "Please enter a password.", Toast.LENGTH_SHORT).show()
                            return@BrutalistButton
                        }
                        if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                            Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                            return@BrutalistButton
                        }

                        stage = 3
                        progressVal = 0.5f

                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val tempFile = File.createTempFile("protected_pdf_", ".pdf", context.cacheDir)
                                tempPdfFile = tempFile

                                FileOutputStream(tempFile).use { fos ->
                                    PdfUtils.protectPdf(
                                        context = context,
                                        pdfUri = selectedPdfUri!!,
                                        password = java.lang.String(password),
                                        outputStream = fos
                                    )
                                }

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Document Protected!", Toast.LENGTH_SHORT).show()
                                    stage = 4
                                }
                            } catch (e: Exception) {
                                tempPdfFile?.delete()
                                tempPdfFile = null
                                withContext(Dispatchers.Main) {
                                    errorMessage = "Failed to load PDF. Is it a valid, non-encrypted file?"
                                    stage = 5
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    backgroundColor = FlippYellow,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            3 -> {
                // State 4: Processing
                ProcessingStageCard(
                    progress = progressVal,
                    statusText = "Signing keys... Encrypting pages natively...",
                    stepDetails = "Applying 256-bit AES encryption locally on your device"
                )
            }

            4 -> {
                // State 5: Completed
                CompletedHeroCard(
                    title = "🎉 PDF Locked Successfully!",
                    subtitle = "Your PDF has been encrypted with 256-bit AES protection.",
                    primaryButtonText = "Download Combined PDF 📁",
                    onPrimaryClick = { pdfSaver.launch("encrypted_document.pdf") },
                    secondaryButtonText = "Convert another file ↺",
                    onSecondaryClick = {
                        selectedPdfUri = null
                        password = ""
                        confirmPassword = ""
                        tempPdfFile?.delete()
                        tempPdfFile = null
                        stage = 1
                    },
                    accentColor = MintColor
                )
            }

            5 -> {
                // Error State
                ErrorCard(
                    errorMessage = errorMessage.ifEmpty { "Failed to encrypt the selected PDF document." },
                    onDismissOrRetry = {
                        stage = if (selectedPdfUri != null) 2 else 1
                    },
                    retryButtonText = "Try Again"
                )
            }
        }
    }
}
