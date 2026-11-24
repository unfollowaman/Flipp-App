package com.example

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
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

        Box(modifier = modifier, propagateMinConstraints = true) {
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
                content = content
            )
        }
    } else {
        Box(modifier = modifier, propagateMinConstraints = true) {
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
                .padding(vertical = 14.dp, horizontal = 24.dp)
                .align(Alignment.Center),
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
    browseText: String = "browse files",
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

// ---------------------- REUSABLE FEATURE-PAGE ARCHITECTURE ----------------------

/**
 * Universal Neobrutalist Scaffold for all Flipp feature screens.
 * Implements clean white primary background with controlled color accents,
 * WCAG 2.1 AA accessible navigation, compact tool identity, and subtle privacy reassurance.
 */
@Composable
fun FeaturePageScaffold(
    title: String,
    desc: String,
    badgeLabel: String,
    onBack: () -> Unit,
    badgeColor: Color = YellowColor,
    privacyReassurance: String = "100% on-device • Never leaves your phone",
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteColor)
    ) {
        // App Bar
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
                    .size(40.dp)
                    .background(WhiteColor, RoundedCornerShape(8.dp))
                    .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
                    .testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = BlackColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BlackColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            ToolBadge(text = badgeLabel, backgroundColor = badgeColor)
        }

        // Scrollable content body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Compact Tool Identity & Header Card
            BrutalistShadowBox(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = WhiteColor,
                cornerRadius = 14.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor
                        )
                        ToolBadge(text = badgeLabel, backgroundColor = badgeColor)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = desc,
                        fontSize = 13.sp,
                        color = Color(0xFF444444),
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    // Subtle Privacy Reassurance Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(CreamColor, RoundedCornerShape(50))
                            .border(1.dp, BlackColor, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Privacy guaranteed",
                            tint = BlackColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = privacyReassurance,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlackColor
                        )
                    }
                }
            }

            content()
        }
    }
}

/**
 * State 2 (File Selected): Represents the chosen document with metadata and actions.
 */
@Composable
fun SelectedFileCard(
    fileName: String,
    modifier: Modifier = Modifier,
    fileSizeStr: String? = null,
    pageCount: Int? = null,
    fileTypeBadge: String = "PDF",
    badgeColor: Color = YellowColor,
    previewBitmap: Bitmap? = null,
    onChangeFile: (() -> Unit)? = null
) {
    BrutalistShadowBox(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = CreamColor,
        cornerRadius = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = "Document thumbnail preview",
                    modifier = Modifier
                        .size(48.dp)
                        .background(WhiteColor, RoundedCornerShape(6.dp))
                        .border(1.5.dp, BlackColor, RoundedCornerShape(6.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(WhiteColor, RoundedCornerShape(8.dp))
                        .border(2.dp, BlackColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = fileTypeBadge,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = BlackColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlackColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ToolBadge(text = fileTypeBadge, backgroundColor = badgeColor)
                    if (pageCount != null && pageCount > 0) {
                        Text(
                            text = "$pageCount pages",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF555555)
                        )
                    }
                    if (fileSizeStr != null) {
                        Text(
                            text = "• $fileSizeStr",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }

            if (onChangeFile != null) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onChangeFile,
                    modifier = Modifier
                        .size(40.dp)
                        .background(WhiteColor, RoundedCornerShape(8.dp))
                        .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
                        .testTag("change_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Change file",
                        tint = BlackColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * State 3: Neobrutalist option pill for settings. Meets WCAG 2.1 AA touch target standards (>= 48dp).
 */
@Composable
fun OptionPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtext: String? = null,
    selectedColor: Color = FlippYellow
) {
    val backgroundColor = if (selected) selectedColor else WhiteColor
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .background(backgroundColor, shape)
            .border(2.dp, BlackColor, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                color = BlackColor
            )
            if (subtext != null) {
                Text(
                    text = subtext,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) BlackColor.copy(alpha = 0.8f) else Color(0xFF777777)
                )
            }
        }
    }
}

/**
 * State 3: Horizontally grouped pill selector.
 */
@Composable
fun <T> NeobrutalistPillGroup(
    title: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier,
    subtextProvider: ((T) -> String?)? = null,
    selectedColor: Color = FlippYellow
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = BlackColor,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                OptionPill(
                    label = labelProvider(option),
                    subtext = subtextProvider?.invoke(option),
                    selected = isSelected,
                    selectedColor = selectedColor,
                    onClick = { onOptionSelected(option) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * State 3: Progressive-disclosure expandable settings container.
 */
@Composable
fun CollapsibleSettingsCard(
    modifier: Modifier = Modifier,
    title: String = "Settings & Options",
    initiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    var isExpanded by rememberSaveable { mutableStateOf(initiallyExpanded) }

    BrutalistShadowBox(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = WhiteColor,
        cornerRadius = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (isExpanded) "Collapse settings" else "Expand settings",
                        onClick = { isExpanded = !isExpanded }
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlackColor
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = BlackColor
                )
            }
            if (isExpanded) {
                Spacer(modifier = Modifier.height(14.dp))
                content()
            }
        }
    }
}

/**
 * State 3: Position Grid Selector for watermarking & page numbering.
 */
@Composable
fun PositionGridSelector(
    selectedPosition: String,
    onPositionSelected: (String) -> Unit,
    positions: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "POSITION",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = BlackColor,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        val chunked = positions.chunked(3)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            chunked.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { (key, label) ->
                        val isSelected = key == selectedPosition
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .selectable(
                                    selected = isSelected,
                                    role = Role.RadioButton,
                                    onClick = { onPositionSelected(key) }
                                )
                                .background(if (isSelected) FlippYellow else WhiteColor, RoundedCornerShape(8.dp))
                                .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = BlackColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * State 1 (Idle): Explanatory How It Works guide card.
 */
@Composable
fun FeatureHowItWorksCard(
    modifier: Modifier = Modifier,
    step1: String = "Select your file from device",
    step2: String = "Customize processing options",
    step3: String = "Export and save locally"
) {
    BrutalistShadowBox(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = CreamColor,
        cornerRadius = 12.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                text = "HOW IT WORKS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = BlackColor,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            listOf("1" to step1, "2" to step2, "3" to step3).forEach { (num, text) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(FlippYellow, RoundedCornerShape(50))
                            .border(1.5.dp, BlackColor, RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = num, fontSize = 11.sp, fontWeight = FontWeight.Black, color = BlackColor)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = BlackColor)
                }
            }
        }
    }
}

/**
 * State 4 (Processing): Dedicated card showing progress, label, and device privacy assurance.
 */
@Composable
fun ProcessingStageCard(
    progress: Float,
    statusText: String,
    modifier: Modifier = Modifier,
    stepDetails: String? = null
) {
    BrutalistShadowBox(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = WhiteColor,
        cornerRadius = 14.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = statusText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = BlackColor,
                textAlign = TextAlign.Center
            )
            if (stepDetails != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stepDetails,
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            StageProgressBar(progress = progress, label = statusText)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "On-device processing",
                    tint = BlackColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Processing locally on your device",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF555555)
                )
            }
        }
    }
}

/**
 * State 5 (Completed): High-contrast results celebration hero card.
 */
@Composable
fun CompletedHeroCard(
    modifier: Modifier = Modifier,
    title: String = "✓ Conversion complete!",
    subtitle: String = "Your output is ready to download.",
    primaryButtonText: String = "Download Output 📁",
    onPrimaryClick: () -> Unit,
    secondaryButtonText: String = "Convert Another ↺",
    onSecondaryClick: () -> Unit,
    accentColor: Color = MintColor
) {
    BrutalistShadowBox(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = accentColor,
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = BlackColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color(0xFF222222),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            BrutalistButton(
                text = primaryButtonText,
                onClick = onPrimaryClick,
                backgroundColor = FlippYellow,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            BrutalistButton(
                text = secondaryButtonText,
                onClick = onSecondaryClick,
                backgroundColor = WhiteColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * State 5 (Completed): Individual page/file result card for bulk tools (PDF to PNG, Split PDF).
 */
@Composable
fun IndividualOutputCard(
    title: String,
    bitmap: Bitmap?,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Page thumbnail for $title",
                    modifier = Modifier
                        .size(56.dp)
                        .background(CreamColor, RoundedCornerShape(6.dp))
                        .border(1.5.dp, BlackColor, RoundedCornerShape(6.dp))
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = BlackColor,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            BrutalistButton(
                text = "Save",
                onClick = onSave,
                backgroundColor = FlippYellow,
                modifier = Modifier.widthIn(min = 90.dp)
            )
        }
    }
}

/**
 * Error State Card: High-contrast error message and retry action.
 */
@Composable
fun ErrorCard(
    errorMessage: String,
    modifier: Modifier = Modifier,
    onDismissOrRetry: (() -> Unit)? = null,
    retryButtonText: String = "Try Again"
) {
    BrutalistShadowBox(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = Color(0xFFFFEBEA),
        cornerRadius = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "⚠️ Operation Failed",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = RedColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorMessage,
                fontSize = 13.sp,
                color = BlackColor,
                lineHeight = 18.sp
            )
            if (onDismissOrRetry != null) {
                Spacer(modifier = Modifier.height(14.dp))
                BrutalistButton(
                    text = retryButtonText,
                    onClick = onDismissOrRetry,
                    backgroundColor = WhiteColor
                )
            }
        }
    }
}

/**
 * Item card for multi-file tools (Merge PDF, Images to PDF) supporting reordering and removal.
 */
@Composable
fun MultiFileQueueCard(
    index: Int,
    fileName: String,
    modifier: Modifier = Modifier,
    fileSizeStr: String? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    canMoveUp: Boolean = true,
    canMoveDown: Boolean = true
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(FlippYellow, RoundedCornerShape(6.dp))
                    .border(1.5.dp, BlackColor, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = BlackColor
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlackColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (fileSizeStr != null) {
                    Text(
                        text = fileSizeStr,
                        fontSize = 11.sp,
                        color = Color(0xFF666666)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onMoveUp != null) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = canMoveUp,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move item ${index + 1} up",
                            tint = if (canMoveUp) BlackColor else Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (onMoveDown != null) {
                    IconButton(
                        onClick = onMoveDown,
                        enabled = canMoveDown,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move item ${index + 1} down",
                            tint = if (canMoveDown) BlackColor else Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (onRemove != null) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove item ${index + 1}",
                            tint = RedColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive thumbnail card for page management (Delete PDF Pages, Rearrange PDF).
 */
@Composable
fun InteractivePageThumbnailCard(
    pageIndex: Int,
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
    isMarkedForDeletion: Boolean = false,
    onDeleteToggle: (() -> Unit)? = null
) {
    val backgroundColor = if (isMarkedForDeletion) Color(0xFFFFD2D2) else WhiteColor
    BrutalistShadowBox(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = backgroundColor,
        cornerRadius = 10.dp
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
                    .background(CreamColor, RoundedCornerShape(6.dp))
                    .border(1.5.dp, BlackColor, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Thumbnail for page ${pageIndex + 1}",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "Page ${pageIndex + 1}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor
                    )
                }
                if (isMarkedForDeletion) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Red.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕ DELETED",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
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
                    text = "Page ${pageIndex + 1}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = BlackColor
                )
                if (onDeleteToggle != null) {
                    IconButton(
                        onClick = onDeleteToggle,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = if (isMarkedForDeletion) "Restore page ${pageIndex + 1}" else "Delete page ${pageIndex + 1}",
                            tint = if (isMarkedForDeletion) Color.Gray else RedColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
