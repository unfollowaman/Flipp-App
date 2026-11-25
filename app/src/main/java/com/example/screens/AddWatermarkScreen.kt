package com.example

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlackColor
import com.example.ui.theme.FlippYellow
import com.example.ui.theme.MintColor
import com.example.ui.theme.SkyBlueColor
import com.example.ui.theme.WhiteColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun AddWatermarkScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1: Idle, 2: File Selected & Settings, 3: Processing, 4: Completed, 5: Error
    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSizeStr by remember { mutableStateOf<String?>(null) }
    var baseImageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var watermarkType by rememberSaveable { mutableStateOf("text") }
    var watermarkText by rememberSaveable { mutableStateOf("CONFIDENTIAL") }
    var watermarkImageUri by remember { mutableStateOf<Uri?>(null) }
    var textColorHex by rememberSaveable { mutableStateOf("#000000") }
    var opacity by rememberSaveable { mutableFloatStateOf(0.5f) }
    var size by rememberSaveable { mutableFloatStateOf(48f) }
    var rotation by rememberSaveable { mutableFloatStateOf(0f) }
    var position by rememberSaveable { mutableStateOf("center") }

    var progressVal by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf("") }
    var tempResultFile by remember { mutableStateOf<File?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            tempResultFile?.delete()
            baseImageBitmap?.recycle()
        }
    }

    val watermarkImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                watermarkImageUri = uri
                Toast.makeText(context, "Watermark image selected!", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val fileSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg"),
        onResult = { uri ->
            if (uri != null && tempResultFile != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            tempResultFile!!.inputStream().use { input ->
                                input.copyTo(out)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Watermarked file saved successfully!", Toast.LENGTH_SHORT).show()
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
        title = "Add Watermark",
        desc = "Stamp images or text.",
        badgeLabel = "WATERMARK",
        onBack = onBack,
        badgeColor = SkyBlueColor
    ) {
        when (stage) {
            1 -> {
                // State 1: Idle
                SingleFileDropZone(
                    mimeTypes = arrayOf("image/*"),
                    prompt = "Please select an image file to watermark.",
                    badgeColor = SkyBlueColor,
                    onFileSelected = { uri ->
                        selectedImageUri = uri
                        fileName = FileUtils.getFileName(context, uri)
                        fileSizeStr = FileUtils.getFileSizeString(context, uri)

                        coroutineScope.launch {
                            val thumb = withContext(Dispatchers.IO) {
                                loadImageThumbnail(context, uri)
                            }
                            baseImageBitmap = thumb
                            stage = 2
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                FeatureHowItWorksCard(
                    step1 = "Select base image from your device",
                    step2 = "Customize watermark text or logo with position and opacity",
                    step3 = "Export and download high-resolution watermarked image"
                )
            }

            2 -> {
                // State 2 & 3: File Selected & Settings
                SelectedFileCard(
                    fileName = fileName.ifEmpty { "image.jpg" },
                    fileSizeStr = fileSizeStr,
                    fileTypeBadge = "IMG",
                    badgeColor = SkyBlueColor,
                    previewBitmap = baseImageBitmap,
                    onChangeFile = {
                        selectedImageUri = null
                        baseImageBitmap = null
                        stage = 1
                    }
                )

                CollapsibleSettingsCard(title = "Watermark Options") {
                    NeobrutalistPillGroup(
                        title = "WATERMARK TYPE",
                        options = listOf("text", "image"),
                        selectedOption = watermarkType,
                        onOptionSelected = { watermarkType = it },
                        labelProvider = { if (it == "text") "Text Watermark" else "Image Watermark" },
                        selectedColor = SkyBlueColor
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (watermarkType == "text") {
                        Text(
                            text = "Watermark Text:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlackColor,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = watermarkText,
                            onValueChange = { watermarkText = it },
                            placeholder = { Text("Enter text...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Watermark text content" },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BlackColor,
                                unfocusedBorderColor = BlackColor
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Text Color:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlackColor,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WATERMARK_COLORS_LIST.forEach { (colorHex, colorName) ->
                                val isSelected = textColorHex == colorHex
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .selectable(
                                            selected = isSelected,
                                            role = Role.RadioButton,
                                            onClick = { textColorHex = colorHex }
                                        )
                                        .background(
                                            if (isSelected) FlippYellow else WhiteColor,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = colorName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        color = BlackColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Font Size: ${size.toInt()} pt",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlackColor
                        )
                        Slider(
                            value = size,
                            onValueChange = { size = it },
                            valueRange = 16f..120f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Watermark font size" },
                            colors = SliderDefaults.colors(
                                thumbColor = BlackColor,
                                activeTrackColor = BlackColor
                            )
                        )
                    } else {
                        Text(
                            text = "Watermark Logo Image:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlackColor,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        BrutalistButton(
                            text = if (watermarkImageUri != null) "✓ Watermark Image Selected" else "Select Watermark Image",
                            onClick = { watermarkImagePicker.launch(arrayOf("image/*")) },
                            backgroundColor = if (watermarkImageUri != null) MintColor else WhiteColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    PositionGridSelector(
                        selectedPosition = position,
                        onPositionSelected = { position = it },
                        positions = WATERMARK_POSITIONS_LIST
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Opacity: ${(opacity * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor
                    )
                    Slider(
                        value = opacity,
                        onValueChange = { opacity = it },
                        valueRange = 0.1f..1.0f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Watermark opacity" },
                        colors = SliderDefaults.colors(
                            thumbColor = BlackColor,
                            activeTrackColor = BlackColor
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Rotation: ${rotation.toInt()}°",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor
                    )
                    Slider(
                        value = rotation,
                        onValueChange = { rotation = it },
                        valueRange = 0f..360f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Watermark rotation" },
                        colors = SliderDefaults.colors(
                            thumbColor = BlackColor,
                            activeTrackColor = BlackColor
                        )
                    )
                }

                BrutalistButton(
                    text = "Apply Watermark →",
                    onClick = {
                        if (watermarkType == "image" && watermarkImageUri == null) {
                            Toast.makeText(context, "Please select a watermark image first.", Toast.LENGTH_SHORT).show()
                            return@BrutalistButton
                        }

                        stage = 3
                        progressVal = 0.5f

                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val tempFile = File.createTempFile("watermarked_", ".jpg", context.cacheDir)
                                tempResultFile = tempFile

                                FileOutputStream(tempFile).use { fos ->
                                    ImageUtils.addWatermark(
                                        context = context,
                                        baseImageUri = selectedImageUri!!,
                                        watermarkType = watermarkType,
                                        watermarkText = watermarkText,
                                        watermarkImageUri = watermarkImageUri,
                                        position = position,
                                        opacity = opacity,
                                        rotation = rotation,
                                        size = size,
                                        colorStr = textColorHex,
                                        outputStream = fos
                                    )
                                }

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Watermark applied successfully!", Toast.LENGTH_SHORT).show()
                                    stage = 4
                                }
                            } catch (e: Exception) {
                                tempResultFile?.delete()
                                tempResultFile = null
                                withContext(Dispatchers.Main) {
                                    errorMessage = "Watermarking failed: ${e.message ?: "Invalid image"}"
                                    stage = 5
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    backgroundColor = SkyBlueColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            3 -> {
                // State 4: Processing
                ProcessingStageCard(
                    progress = progressVal,
                    statusText = "Applying watermark...",
                    stepDetails = "Rendering watermark with Matrix hardware transformation locally"
                )
            }

            4 -> {
                // State 5: Completed
                CompletedHeroCard(
                    title = "💧 Watermark Applied!",
                    subtitle = "Your watermarked image is ready to download.",
                    primaryButtonText = "Download Watermarked Image 📁",
                    onPrimaryClick = { fileSaver.launch("watermarked_image.jpg") },
                    secondaryButtonText = "Watermark another file ↺",
                    onSecondaryClick = {
                        selectedImageUri = null
                        baseImageBitmap = null
                        tempResultFile?.delete()
                        tempResultFile = null
                        stage = 1
                    },
                    accentColor = MintColor
                )
            }

            5 -> {
                // Error State
                ErrorCard(
                    errorMessage = errorMessage.ifEmpty { "Failed to watermark the selected image." },
                    onDismissOrRetry = {
                        stage = if (selectedImageUri != null) 2 else 1
                    },
                    retryButtonText = "Adjust Watermark Settings"
                )
            }
        }
    }
}
