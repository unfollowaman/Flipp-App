package com.example
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.ui.theme.*
import com.example.ui.theme.CreamColor
import com.example.ui.theme.WhiteColor

@Composable
fun BrutalistShadowBox(
    modifier: Modifier = Modifier,
    backgroundColor: Color = WhiteColor,
    shadowColor: Color = BlackColor,
    cornerRadius: Dp = 10.dp,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    testTag: String? = null,
    borderWidth: Dp = 2.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val offsetTransition by animateDpAsState(
            targetValue = if (isPressed) 0.dp else 4.dp,
            animationSpec = spring(stiffness = Spring.StiffnessHigh),
            label = "offsetAnimation"
        )

        Box(
            modifier = modifier,
            propagateMinConstraints = true
        ) {
            // Shadow
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(4.dp, 4.dp)
                    .background(shadowColor, shape)
            )
            // Pressed offset foreground
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = (offsetTransition - 4.dp).toPx()
                        translationY = (offsetTransition - 4.dp).toPx()
                    }
                    .testTag(testTag ?: "")
                    .background(backgroundColor, shape)
                    .border(borderWidth, shadowColor, shape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClickLabel = onClickLabel,
                        onClick = onClick
                    ),
                propagateMinConstraints = true,
                content = content
            )
        }
    } else {
        Box(
            modifier = modifier,
            propagateMinConstraints = true
        ) {
            // Shadow
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(4.dp, 4.dp)
                    .background(shadowColor, shape)
            )
            // Foreground
            Box(
                modifier = Modifier
                    .background(backgroundColor, shape)
                    .border(borderWidth, shadowColor, shape),
                propagateMinConstraints = true,
                content = content
            )
        }
    }
}

@Composable
fun BrutalistButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = WhiteColor,
    testTag: String? = null
) {
    BrutalistShadowBox(
        modifier = modifier,
        backgroundColor = backgroundColor,
        cornerRadius = 10.dp,
        onClick = onClick,
        testTag = testTag
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = BlackColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 28.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TopNavbar(
    onPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLogoClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(WhiteColor)
            .bottomBorder(2.dp, BlackColor)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Go to home screen",
                    onClick = { onLogoClick?.invoke() }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "flipp",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = BlackColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "| file converter",
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                color = BlackColor.copy(alpha = 0.5f)
            )
        }
        
        // Circular privacy Settings-like Cog button from the design HTML
        IconButton(
            onClick = onPrivacyClick,
            modifier = Modifier
                .size(40.dp)
                .background(CreamColor, RoundedCornerShape(20.dp))
                .border(2.dp, BlackColor, RoundedCornerShape(20.dp))
                .testTag("privacy_navbar_button")
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Privacy Commitment",
                tint = BlackColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun Modifier.bottomBorder(bottom: Dp, color: Color): Modifier = this.drawBehind {
    val strokeWidth = bottom.toPx()
    val y = size.height - strokeWidth / 2
    drawLine(
        color = color,
        start = androidx.compose.ui.geometry.Offset(0f, y),
        end = androidx.compose.ui.geometry.Offset(size.width, y),
        strokeWidth = strokeWidth
    )
}

@Composable
fun BottomTrustBar() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Redesigned privacy bar from design HTML
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BlackColor)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LOCAL ONLY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = com.example.ui.theme.MintColor,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = "100% Privacy-First",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhiteColor
                )
            }
            // Trust element: Sky blue box with shield-like lock icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(com.example.ui.theme.SkyBlueColor, RoundedCornerShape(12.dp))
                    .border(1.dp, WhiteColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Privacy protection",
                    tint = BlackColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // Cream-colored disclaimer bar with uppercase tracking
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CreamColor)
                .topBorder(2.dp, BlackColor)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "NO UPLOADS • NO ACCOUNTS • WORKS OFFLINE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = BlackColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun Modifier.topBorder(top: Dp, color: Color): Modifier = this.drawBehind {
    val strokeWidth = top.toPx()
    val y = strokeWidth / 2
    drawLine(
        color = color,
        start = androidx.compose.ui.geometry.Offset(0f, y),
        end = androidx.compose.ui.geometry.Offset(size.width, y),
        strokeWidth = strokeWidth
    )
}

@Composable
fun ToolBadge(
    text: String,
    backgroundColor: Color
) {
    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(50))
            .border(1.dp, BlackColor, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = BlackColor
        )
    }
}

private val dropZonePathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)

@Composable
fun DropZone(
    onBrowseClick: () -> Unit,
    prompt: String = "Drag files here or",
    browseText: String = "Browse Files",
    errorText: String? = null,
    badgeColor: Color = WhiteColor
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color(0xFFFAFAFA), RoundedCornerShape(16.dp))
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                drawRoundRect(
                    color = BlackColor,
                    style = Stroke(width = strokeWidth, pathEffect = dropZonePathEffect),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                )
            }
            .clickable(
                role = Role.Button,
                onClickLabel = "Select files",
                onClick = onBrowseClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Upload icon",
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .size(36.dp),
                tint = BlackColor
            )
            
            Text(
                text = prompt,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = BlackColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Accepts .pdf or images",
                fontSize = 11.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp, top = 4.dp)
            )
            
            // browse files button
            BrutalistShadowBox(
                modifier = Modifier,
                backgroundColor = WhiteColor,
                cornerRadius = 10.dp,
                onClick = onBrowseClick,
                testTag = "browse_files_button",
                borderWidth = 2.dp
            ) {
                Text(
                    text = browseText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlackColor,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            
            errorText?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = RedColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SingleFileDropZone(
    onFileSelected: (Uri) -> Unit,
    mimeTypes: Array<String> = arrayOf("application/pdf"),
    prompt: String = "Please select a file.",
    badgeColor: Color = WhiteColor,
    onCancelledMessage: String = "Please select a file."
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                onFileSelected(uri)
            } else if (onCancelledMessage.isNotEmpty()) {
                Toast.makeText(context, onCancelledMessage, Toast.LENGTH_SHORT).show()
            }
        }
    )

    DropZone(
        onBrowseClick = { launcher.launch(mimeTypes) },
        prompt = prompt,
        badgeColor = badgeColor
    )
}

@Composable
fun StageProgressBar(
    progress: Float,
    label: String
) {
    BrutalistShadowBox(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFFFAFAFA),
        cornerRadius = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlackColor
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlackColor
                )
            }
            
            // Custom brutalist progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .progressSemantics(progress.coerceIn(0f, 1f))
                    .background(WhiteColor, RoundedCornerShape(99.dp))
                    .border(2.dp, BlackColor, RoundedCornerShape(99.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(MintColor, RoundedCornerShape(99.dp))
                )
            }
        }
    }
}

@Composable
fun OptionPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sublabel: String? = null,
    badgeText: String? = null,
    activeColor: Color = BlackColor,
    activeTextColor: Color = WhiteColor,
    inactiveColor: Color = WhiteColor,
    inactiveTextColor: Color = BlackColor,
    testTag: String? = null
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(10.dp)
    
    val currentBg = if (selected) activeColor else inactiveColor
    val currentText = if (selected) activeTextColor else inactiveTextColor
    
    val shadowOffset by animateDpAsState(
        targetValue = if (isPressed) 0.dp else 2.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "pillShadow"
    )

    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } catch (_: Exception) {}
                    onClick()
                }
            )
            .testTag(testTag ?: "")
    ) {
        // Hard shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(shadowOffset, shadowOffset)
                .background(BlackColor, shape)
        )
        // Foreground pill
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = (shadowOffset - 2.dp).toPx()
                    translationY = (shadowOffset - 2.dp).toPx()
                }
                .background(currentBg, shape)
                .border(2.dp, BlackColor, shape)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentText,
                        textAlign = TextAlign.Center
                    )
                    if (!sublabel.isNullOrEmpty()) {
                        Text(
                            text = sublabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            color = currentText.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (!badgeText.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (selected) WhiteColor else YellowColor,
                                RoundedCornerShape(4.dp)
                            )
                            .border(1.dp, BlackColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun <T> NeobrutalistPillSelector(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelProvider: (T) -> String = { it.toString() },
    sublabelProvider: ((T) -> String)? = null,
    badgeProvider: ((T) -> String)? = null,
    activeColor: Color = BlackColor,
    activeTextColor: Color = WhiteColor,
    inactiveColor: Color = WhiteColor,
    inactiveTextColor: Color = BlackColor
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            OptionPill(
                label = labelProvider(option),
                sublabel = sublabelProvider?.invoke(option),
                badgeText = badgeProvider?.invoke(option),
                selected = isSelected,
                onClick = { onOptionSelected(option) },
                activeColor = activeColor,
                activeTextColor = activeTextColor,
                inactiveColor = inactiveColor,
                inactiveTextColor = inactiveTextColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DocumentSummaryHeader(
    filename: String,
    pageCount: Int,
    modifier: Modifier = Modifier,
    fileSizeString: String? = null,
    backgroundColor: Color = CreamColor,
    onRemoveClick: (() -> Unit)? = null
) {
    BrutalistShadowBox(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = backgroundColor,
        cornerRadius = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(YellowColor, RoundedCornerShape(8.dp))
                        .border(2.dp, BlackColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📄",
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = filename,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = BlackColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ToolBadge(
                            text = if (pageCount == 1) "1 PAGE" else "$pageCount PAGES",
                            backgroundColor = WhiteColor
                        )
                        if (!fileSizeString.isNullOrEmpty()) {
                            ToolBadge(
                                text = fileSizeString,
                                backgroundColor = WhiteColor
                            )
                        }
                    }
                }
            }
            if (onRemoveClick != null) {
                IconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(WhiteColor, RoundedCornerShape(8.dp))
                        .border(1.5.dp, BlackColor, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove file",
                        tint = BlackColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentSummaryHeader(
    fileName: String,
    pageCount: Int,
    fileSize: String? = null,
    accentColor: Color = CreamColor,
    modifier: Modifier = Modifier,
    onRemoveClick: (() -> Unit)? = null
) {
    DocumentSummaryHeader(
        filename = fileName,
        pageCount = pageCount,
        modifier = modifier,
        fileSizeString = fileSize,
        backgroundColor = accentColor,
        onRemoveClick = onRemoveClick
    )
}

@Composable
fun PageThumbnailGrid(
    thumbnails: List<Bitmap>,
    modifier: Modifier = Modifier,
    selectedPages: Set<Int>? = null,
    onPageClick: ((Int) -> Unit)? = null,
    onDeletePage: ((Int) -> Unit)? = null,
    pageLabelProvider: ((Int) -> String)? = null
) {
    if (thumbnails.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        val rows = (thumbnails.indices step 2).toList()
        for (startIndex in rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val index1 = startIndex
                val isSelected1 = selectedPages?.contains(index1) ?: false
                ThumbnailCard(
                    pageIndex = index1,
                    bitmap = thumbnails[index1],
                    label = pageLabelProvider?.invoke(index1) ?: "Page ${index1 + 1}",
                    isSelected = isSelected1,
                    onClick = onPageClick?.let { { it(index1) } },
                    onDelete = onDeletePage?.let { { it(index1) } },
                    modifier = Modifier.weight(1f)
                )

                if (startIndex + 1 < thumbnails.size) {
                    val index2 = startIndex + 1
                    val isSelected2 = selectedPages?.contains(index2) ?: false
                    ThumbnailCard(
                        pageIndex = index2,
                        bitmap = thumbnails[index2],
                        label = pageLabelProvider?.invoke(index2) ?: "Page ${index2 + 1}",
                        isSelected = isSelected2,
                        onClick = onPageClick?.let { { it(index2) } },
                        onDelete = onDeletePage?.let { { it(index2) } },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ThumbnailCard(
    pageIndex: Int,
    bitmap: Bitmap,
    label: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    BrutalistShadowBox(
        modifier = modifier,
        backgroundColor = if (isSelected) YellowColor.copy(alpha = 0.25f) else WhiteColor,
        cornerRadius = 10.dp,
        onClick = onClick,
        onClickLabel = "Select $label"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFFEEEEEE), RoundedCornerShape(6.dp))
                    .border(1.dp, BlackColor, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize()
                )
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(28.dp)
                            .background(RedColor, RoundedCornerShape(4.dp))
                            .border(1.dp, BlackColor, RoundedCornerShape(4.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete $label",
                            tint = WhiteColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlackColor
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(MintColor, RoundedCornerShape(9.dp))
                            .border(1.dp, BlackColor, RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = BlackColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResultsHeroBanner(
    title: String = "✓ Conversion complete!",
    subtitle: String? = "Your files are ready to download.",
    primaryButtonText: String = "Download All as ZIP 📦",
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryButtonText: String? = "Convert another file ↺",
    onSecondaryClick: (() -> Unit)? = null,
    bannerColor: Color = MintColor
) {
    BrutalistShadowBox(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = bannerColor,
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = BlackColor,
                textAlign = TextAlign.Center
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = BlackColor.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            BrutalistButton(
                text = primaryButtonText,
                onClick = onPrimaryClick,
                backgroundColor = YellowColor,
                modifier = Modifier.fillMaxWidth()
            )
            if (!secondaryButtonText.isNullOrEmpty() && onSecondaryClick != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .clickable(
                            role = Role.Button,
                            onClickLabel = secondaryButtonText,
                            onClick = onSecondaryClick
                        )
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = secondaryButtonText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ResultsHeroBanner(
    title: String,
    subtitle: String?,
    primaryCtaText: String,
    onPrimaryCta: () -> Unit,
    onReset: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    ResultsHeroBanner(
        title = title,
        subtitle = subtitle,
        primaryButtonText = primaryCtaText,
        onPrimaryClick = onPrimaryCta,
        secondaryButtonText = "Convert another file ↺",
        onSecondaryClick = onReset,
        bannerColor = accentColor,
        modifier = modifier
    )
}

@Composable
fun IndividualOutputCard(
    pageIndex: Int,
    bitmap: Bitmap?,
    label: String,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    onShareClick: (() -> Unit)? = null,
    buttonText: String = "Download"
) {
    BrutalistShadowBox(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = WhiteColor,
        cornerRadius = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp, 70.dp)
                        .background(Color(0xFFEEEEEE), RoundedCornerShape(6.dp))
                        .border(1.5.dp, BlackColor, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = label,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = "🖼️",
                            fontSize = 24.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = BlackColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Page ${pageIndex + 1}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = BlackColor.copy(alpha = 0.6f)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onShareClick != null) {
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier
                            .size(38.dp)
                            .background(CreamColor, RoundedCornerShape(8.dp))
                            .border(1.5.dp, BlackColor, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share $label",
                            tint = BlackColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                BrutalistShadowBox(
                    backgroundColor = YellowColor,
                    cornerRadius = 8.dp,
                    onClick = onSaveClick,
                    borderWidth = 1.5.dp
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = BlackColor,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ToolScreenTemplate(
    title: String,
    desc: String,
    badgeLabel: String,
    onBack: () -> Unit,
    badgeColor: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(WhiteColor)
                .bottomBorder(2.dp, BlackColor)
                .padding(horizontal = 16.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .background(WhiteColor, RoundedCornerShape(6.dp))
                    .border(2.dp, BlackColor, RoundedCornerShape(6.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = BlackColor
                )
            }
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BlackColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
                letterSpacing = (-0.02).em
            )
            ToolBadge(text = badgeLabel, backgroundColor = badgeColor)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BrutalistShadowBox(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = badgeColor,
                cornerRadius = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = BlackColor,
                        letterSpacing = (-0.02).em,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = desc,
                        fontSize = 12.sp,
                        color = Color(0xFF333333),
                        lineHeight = 18.sp
                    )
                }
            }
            content()
        }
    }
}

fun loadPdfThumbnails(context: Context, pdfUri: Uri, maxPages: Int = 4): List<Bitmap> {
    val bitmaps = mutableListOf<Bitmap>()
    try {
        context.contentResolver.openFileDescriptor(pdfUri, "r")?.use { pfd ->
            val renderer = try {
                PdfRenderer(pfd)
            } catch (e: SecurityException) {
                return emptyList()
            }
            val pagesToRender = minOf(renderer.pageCount, maxPages)
            for (i in 0 until pagesToRender) {
                val page = renderer.openPage(i)
                val width = page.width / 2
                val height = page.height / 2
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
                page.close()
            }
            renderer.close()
        }
    } catch (e: Exception) {
        AppLogger.e("UiComponents", "Error loading PDF thumbnails", e)
    }
    return bitmaps
}

fun loadPdfPageBitmap(context: Context, pdfUri: Uri, pageIndex: Int): Bitmap? {
    return try {
        context.contentResolver.openFileDescriptor(pdfUri, "r")?.use { pfd ->
            val renderer = PdfRenderer(pfd)
            if (pageIndex in 0 until renderer.pageCount) {
                val page = renderer.openPage(pageIndex)
                val width = page.width / 2
                val height = page.height / 2
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                bitmap
            } else {
                renderer.close()
                null
            }
        }
    } catch (e: Exception) {
        AppLogger.e("UiComponents", "Error rendering PDF page", e)
        null
    }
}

fun loadImageThumbnail(context: Context, uri: Uri): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply {
            inSampleSize = 8
        }
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        }
    } catch (e: Exception) {
        AppLogger.e("UiComponents", "Error loading image thumbnail", e)
        null
    }
}
