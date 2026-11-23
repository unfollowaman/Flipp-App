package com.example

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun UnlockPdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfFileName by remember { mutableStateOf("encrypted.pdf") }

    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }

    var isProcessing by remember { mutableStateOf(false) }
    var progressVal by remember { mutableFloatStateOf(0f) }
    var progressLabel by remember { mutableStateOf("") }
    var tempUnlockedFile by remember { mutableStateOf<File?>(null) }

    fun inspectUri(uri: Uri) {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    pdfFileName = cursor.getString(nameIndex) ?: "encrypted.pdf"
                }
            }
        } catch (_: Exception) {
            pdfFileName = uri.lastPathSegment ?: "encrypted.pdf"
        }
    }

    val pdfSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            if (uri != null && tempUnlockedFile != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            tempUnlockedFile!!.inputStream().copyTo(out)
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Unlocked PDF saved successfully!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // App Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(WhiteColor)
                .border(2.dp, BlackColor)
                .padding(horizontal = 16.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(WhiteColor, RoundedCornerShape(8.dp))
                    .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = BlackColor
                )
            }
            Text(
                text = "Unlock PDF",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = BlackColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                textAlign = TextAlign.Center,
                letterSpacing = (-0.02).em
            )
            ToolBadge(text = "UNLOCK", backgroundColor = MintColor)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stage 1: File Selection
            if (stage == 1) {
                BrutalistShadowBox(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MintColor,
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Unlock PDF",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = (-0.02).em
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Remove password security and permission restrictions from your PDF document 100% locally.",
                            fontSize = 13.sp,
                            color = Color(0xFF222222),
                            lineHeight = 18.sp
                        )
                    }
                }

                SingleFileDropZone(
                    mimeTypes = arrayOf("application/pdf"),
                    prompt = "Drop your locked PDF here or browse files",
                    badgeColor = MintColor,
                    onFileSelected = { uri ->
                        selectedPdfUri = uri
                        inspectUri(uri)
                        stage = 2
                    }
                )

                // 3-step Guide Card
                BrutalistShadowBox(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = CreamColor,
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "How to unlock a PDF",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        StepItem("01", "Upload PDF", "Select your password-protected PDF document.", MintColor)
                        Spacer(modifier = Modifier.height(10.dp))
                        StepItem("02", "Enter Password", "Provide the document open password to remove encryption.", YellowColor)
                        Spacer(modifier = Modifier.height(10.dp))
                        StepItem("03", "Download Unlocked PDF", "Save your new unrestricted PDF file with zero remote uploads.", SkyBlueColor)
                    }
                }
            }

            // Stage 2: Loaded / Password Prompt
            if (stage == 2) {
                Text(
                    text = "Ready to unlock: $pdfFileName",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = BlackColor
                )

                // Unlock CTA Button
                BrutalistButton(
                    text = "Unlock PDF →",
                    onClick = {
                        if (password.isEmpty()) {
                            Toast.makeText(context, "Please enter the password to unlock", Toast.LENGTH_SHORT).show()
                            return@BrutalistButton
                        }
                        isProcessing = true
                        progressVal = 0.3f
                        progressLabel = "Decrypting PDF document..."
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val tempFile = File.createTempFile("unlocked_", ".pdf", context.cacheDir)
                                tempUnlockedFile = tempFile
                                FileOutputStream(tempFile).use { fos ->
                                    PdfUtils.unlockPdf(context, selectedPdfUri!!, password, fos)
                                }
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    stage = 3
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    Toast.makeText(context, "Decryption failed. Incorrect password.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    backgroundColor = MintColor,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isProcessing) {
                    StageProgressBar(progress = progressVal, label = progressLabel)
                }

                // Password Input Card
                BrutalistShadowBox(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = WhiteColor,
                    cornerRadius = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "THIS PDF IS PASSWORD-PROTECTED. ENTER THE PASSWORD TO UNLOCK IT.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = 0.04.em,
                            lineHeight = 18.sp
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Password input field" },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Text(
                                        text = if (isPasswordVisible) "🐵" else "🙈",
                                        fontSize = 20.sp,
                                        modifier = Modifier.semantics {
                                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                                        }
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BlackColor,
                                unfocusedBorderColor = BlackColor
                            )
                        )
                    }
                }

                // 3-step Guide Card in Stage 2
                BrutalistShadowBox(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = CreamColor,
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "How to unlock a PDF",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        StepItem("01", "Upload PDF", "Drag and drop your PDF into the upload area above.", MintColor)
                        Spacer(modifier = Modifier.height(10.dp))
                        StepItem("02", "Enter Password (If required)", "If the PDF has an open password, enter it to remove restrictions.", YellowColor)
                        Spacer(modifier = Modifier.height(10.dp))
                        StepItem("03", "Download Unlocked PDF", "Save your new PDF without any passwords or permission restrictions.", SkyBlueColor)
                    }
                }
            }

            // Stage 3: Results
            if (stage == 3) {
                ResultsHeroBanner(
                    title = "✓ PDF Unlocked Successfully!",
                    subtitle = "All encryption and permission restrictions have been permanently removed.",
                    primaryButtonText = "Download Unlocked PDF 📄",
                    onPrimaryClick = { pdfSaver.launch("unlocked_${pdfFileName}") },
                    secondaryButtonText = "Unlock another file ↺",
                    onSecondaryClick = {
                        selectedPdfUri = null
                        password = ""
                        tempUnlockedFile = null
                        stage = 1
                    }
                )
            }
        }
    }
}
