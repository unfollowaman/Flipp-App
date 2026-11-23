package com.example

import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlackColor
import com.example.ui.theme.CreamColor
import com.example.ui.theme.MintColor
import com.example.ui.theme.PinkColor
import com.example.ui.theme.SkyBlueColor
import com.example.ui.theme.WhiteColor
import com.example.ui.theme.YellowColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun PdfToWordScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfFileName by rememberSaveable { mutableStateOf("") }
    var pdfFileSize by rememberSaveable { mutableStateOf("") }
    var totalPages by rememberSaveable { mutableIntStateOf(1) }
    val thumbnails = remember { mutableStateListOf<Bitmap>() }

    var conversionMode by rememberSaveable { mutableStateOf("standard") } // "standard", "clean"
    var progressVal by remember { mutableFloatStateOf(0f) }
    var tempDocxFile by remember { mutableStateOf<File?>(null) }

    fun resetState() {
        thumbnails.forEach { it.recycle() }
        thumbnails.clear()
        tempDocxFile?.delete()
        tempDocxFile = null
        selectedPdfUri = null
        pdfFileName = ""
        pdfFileSize = ""
        totalPages = 1
        progressVal = 0f
        stage = 1
    }

    val docxSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        onResult = { uri ->
            if (uri != null && tempDocxFile != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            tempDocxFile!!.inputStream().use { input ->
                                input.copyTo(out)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Word document saved successfully!", Toast.LENGTH_SHORT).show()
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

    fun startConversion() {
        val uri = selectedPdfUri ?: return
        stage = 2
        progressVal = 0.3f

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val tempFile = File(context.cacheDir, "converted_document.docx")
                FileOutputStream(tempFile).use { fos ->
                    PdfUtils.convertPdfToWordDocx(context, uri, fos)
                }

                withContext(Dispatchers.Main) {
                    progressVal = 1f
                    tempDocxFile = tempFile
                    stage = 3
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Conversion failed: ${e.message}", Toast.LENGTH_LONG).show()
                    stage = 2
                }
            }
        }
    }

    ToolScreenTemplate(
        title = "PDF to Word",
        desc = "Convert PDF documents to editable Microsoft Word (.docx) files.",
        badgeLabel = "PDF → WORD",
        onBack = onBack,
        badgeColor = SkyBlueColor
    ) {
        when (stage) {
            1 -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Select PDF Document",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor
                    )
                    Text(
                        text = "Converts locally to standard OpenXML Word .docx format.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )

                    SingleFileDropZone(
                        mimeTypes = arrayOf("application/pdf"),
                        prompt = "Drag PDF here or",
                        onCancelledMessage = "",
                        onFileSelected = { uri ->
                            selectedPdfUri = uri
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                                    cursor?.use {
                                        if (it.moveToFirst()) {
                                            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                                            if (nameIndex != -1) pdfFileName = it.getString(nameIndex) ?: "document.pdf"
                                            if (sizeIndex != -1) {
                                                val bytes = it.getLong(sizeIndex)
                                                pdfFileSize = String.format("%.2f MB", bytes / (1024f * 1024f))
                                            }
                                        }
                                    }
                                } catch (_: Exception) {}

                                try {
                                    val count = PdfUtils.getPdfPageCount(context, uri)
                                    totalPages = count
                                    val thumbs = loadPdfThumbnails(context, uri, maxPages = 4)
                                    withContext(Dispatchers.Main) {
                                        thumbnails.clear()
                                        thumbnails.addAll(thumbs)
                                        stage = 2
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Error opening PDF: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // How it works card
                    BrutalistShadowBox(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = CreamColor,
                        cornerRadius = 16.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "HOW IT WORKS 📄",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF888888),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            StepItem("01", "Select PDF file", "Pick any document from your local storage.", SkyBlueColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            StepItem("02", "Reconstruct layout", "Parses text structure into Word paragraphs and styles.", PinkColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            StepItem("03", "Download DOCX", "Opens seamlessly in Microsoft Word, Google Docs, or LibreOffice.", MintColor)
                        }
                    }
                }
            }

            2 -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DocumentSummaryHeader(
                        fileName = pdfFileName.ifEmpty { "document.pdf" },
                        pageCount = totalPages,
                        fileSize = pdfFileSize.ifEmpty { "PDF" },
                        accentColor = SkyBlueColor
                    )

                    if (progressVal > 0f && progressVal < 1f) {
                        StageProgressBar(progress = progressVal, label = "Reconstructing document in DOCX...")
                    }

                    // Mode Selection
                    BrutalistShadowBox(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = WhiteColor,
                        cornerRadius = 12.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Conversion Preset",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlackColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            NeobrutalistPillSelector(
                                options = listOf("standard", "clean"),
                                selectedOption = conversionMode,
                                onOptionSelected = { conversionMode = it },
                                labelProvider = { if (it == "standard") "Standard (.docx)" else "Clean Text Flow" },
                                sublabelProvider = { if (it == "standard") "Preserves headings & paragraphs" else "Streamlined text for editing" },
                                activeColor = SkyBlueColor
                            )
                        }
                    }

                    // Preview thumbnail grid
                    if (thumbnails.isNotEmpty()) {
                        Text(
                            text = "Page Previews",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlackColor
                        )
                        PageThumbnailGrid(
                            thumbnails = thumbnails,
                            modifier = Modifier.heightIn(max = 240.dp)
                        )
                    }

                    // Convert Action Button
                    BrutalistButton(
                        text = "Convert to Word (.docx) ➔",
                        onClick = { startConversion() },
                        backgroundColor = SkyBlueColor,
                        modifier = Modifier.fillMaxWidth()
                    )

                    BrutalistButton(
                        text = "Select Different File ↺",
                        onClick = { resetState() },
                        backgroundColor = CreamColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            3 -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ResultsHeroBanner(
                        title = "✓ Converted to Word!",
                        subtitle = "Your .docx document is ready to edit in Microsoft Word, Google Docs, or Pages.",
                        primaryCtaText = "Save Word Document (.docx) 📄",
                        onPrimaryCta = {
                            val defaultName = pdfFileName.substringBeforeLast(".") + ".docx"
                            docxSaver.launch(defaultName)
                        },
                        onReset = { resetState() },
                        accentColor = MintColor
                    )

                    BrutalistShadowBox(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = CreamColor,
                        cornerRadius = 16.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "EXPORT SUMMARY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF666666),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Format: Microsoft Word OpenXML (.docx)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlackColor
                            )
                            Text(
                                text = "Source: $pdfFileName ($totalPages pages)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlackColor
                            )
                            Text(
                                text = "Compatibility: Word 2007+, Office 365, Google Docs, LibreOffice",
                                fontSize = 12.sp,
                                color = Color(0xFF555555)
                            )
                        }
                    }

                    BrutalistButton(
                        text = "Convert another file ↺",
                        onClick = { resetState() },
                        backgroundColor = YellowColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
