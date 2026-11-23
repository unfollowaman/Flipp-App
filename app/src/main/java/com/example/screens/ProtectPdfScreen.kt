package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
fun ProtectPdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfFileName by remember { mutableStateOf("document.pdf") }
    var firstPageThumb by remember { mutableStateOf<Bitmap?>(null) }

    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isConfirmVisible by rememberSaveable { mutableStateOf(false) }

    var isProcessing by remember { mutableStateOf(false) }
    var progressVal by remember { mutableFloatStateOf(0f) }
    var progressLabel by remember { mutableStateOf("") }
    var tempProtectedFile by remember { mutableStateOf<File?>(null) }

    fun inspectUri(uri: Uri) {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    pdfFileName = cursor.getString(nameIndex) ?: "document.pdf"
                }
            }
        } catch (_: Exception) {
            pdfFileName = uri.lastPathSegment ?: "document.pdf"
        }
    }

    val pdfSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            if (uri != null && tempProtectedFile != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            tempProtectedFile!!.inputStream().copyTo(out)
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Protected PDF saved successfully!", Toast.LENGTH_SHORT).show()
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
                text = "Protect PDF",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = BlackColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                textAlign = TextAlign.Center,
                letterSpacing = (-0.02).em
            )
            ToolBadge(text = "PROTECT", backgroundColor = AmberColor)
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
                    backgroundColor = AmberColor,
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Protect PDF",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = (-0.02).em
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Encrypt your PDF with standard 256-bit AES encryption. Restrict unauthorized viewing and editing.",
                            fontSize = 13.sp,
                            color = Color(0xFF222222),
                            lineHeight = 18.sp
                        )
                    }
                }

                SingleFileDropZone(
                    mimeTypes = arrayOf("application/pdf"),
                    prompt = "Drop your PDF here or browse files",
                    badgeColor = AmberColor,
                    onFileSelected = { uri ->
                        selectedPdfUri = uri
                        inspectUri(uri)
                        coroutineScope.launch {
                            val thumbs = withContext(Dispatchers.IO) {
                                loadPdfThumbnails(context, uri, maxPages = 1)
                            }
                            firstPageThumb = thumbs.firstOrNull()
                            stage = 2
                        }
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
                            text = "How to password protect a PDF",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        StepItem("1", "Upload your PDF", "Select a PDF document from your device.", AmberColor)
                        Spacer(modifier = Modifier.height(10.dp))
                        StepItem("2", "Set strong password", "Type and confirm your encryption password with monkey eye visibility.", YellowColor)
                        Spacer(modifier = Modifier.height(10.dp))
                        StepItem("3", "Protect & download", "Encrypt using 256-bit AES completely offline.", MintColor)
                    }
                }
            }

            // Stage 2: Configuration & Preview
            if (stage == 2) {
                // Document Preview Card with ORIGINAL PDF badge
                BrutalistShadowBox(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = WhiteColor,
                    cornerRadius = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ToolBadge(text = "ORIGINAL PDF", backgroundColor = MintColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
                                .background(BackgroundColor, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (firstPageThumb != null) {
                                Image(
                                    bitmap = firstPageThumb!!.asImageBitmap(),
                                    contentDescription = "PDF Page 1 Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text("PDF Document", fontWeight = FontWeight.Bold, color = Color.Gray)
                            }
                        }
                    }
                }

                Text(
                    text = "Selected: $pdfFileName",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = BlackColor
                )

                // Password Fields Card
                BrutalistShadowBox(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = WhiteColor,
                    cornerRadius = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "PASSWORD",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = 0.05.em
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
                                IconButton(
                                    onClick = { isPasswordVisible = !isPasswordVisible }
                                ) {
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

                        Text(
                            text = "CONFIRM PASSWORD",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = 0.05.em
                        )
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Confirm password input field" },
                            visualTransformation = if (isConfirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(
                                    onClick = { isConfirmVisible = !isConfirmVisible }
                                ) {
                                    Text(
                                        text = if (isConfirmVisible) "🐵" else "🙈",
                                        fontSize = 20.sp,
                                        modifier = Modifier.semantics {
                                            contentDescription = if (isConfirmVisible) "Hide password" else "Show password"
                                        }
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BlackColor,
                                unfocusedBorderColor = BlackColor
                            )
                        )

                        if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
                            Text(
                                text = "Passwords do not match",
                                color = RedColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Protect CTA Button
                BrutalistButton(
                    text = "Protect PDF →",
                    onClick = {
                        if (password.isEmpty()) {
                            Toast.makeText(context, "Please enter a password", Toast.LENGTH_SHORT).show()
                            return@BrutalistButton
                        }
                        if (password != confirmPassword) {
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                            return@BrutalistButton
                        }
                        isProcessing = true
                        progressVal = 0.2f
                        progressLabel = "Encrypting PDF with AES-256..."
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val tempFile = File.createTempFile("protected_", ".pdf", context.cacheDir)
                                tempProtectedFile = tempFile
                                FileOutputStream(tempFile).use { fos ->
                                    PdfUtils.protectPdf(context, selectedPdfUri!!, java.lang.String(password), fos)
                                }
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    stage = 3
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    Toast.makeText(context, "Encryption failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    backgroundColor = PinkColor,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isProcessing) {
                    StageProgressBar(progress = progressVal, label = progressLabel)
                }
            }

            // Stage 3: Results
            if (stage == 3) {
                ResultsHeroBanner(
                    title = "✓ PDF Protected Successfully!",
                    subtitle = "Your document is now encrypted with 256-bit AES encryption.",
                    primaryButtonText = "Save Protected PDF 📄",
                    onPrimaryClick = { pdfSaver.launch("protected_${pdfFileName}") },
                    secondaryButtonText = "Protect another file ↺",
                    onSecondaryClick = {
                        selectedPdfUri = null
                        firstPageThumb = null
                        password = ""
                        confirmPassword = ""
                        tempProtectedFile = null
                        stage = 1
                    }
                )
            }
        }
    }
}
