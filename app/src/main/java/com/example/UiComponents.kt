package com.example

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        Box(modifier = modifier) {
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
                        onClick = onClick
                    ),
                content = content
            )
        }
    } else {
        Box(modifier = modifier) {
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
                .clickable { onLogoClick?.invoke() },
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
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(CreamColor, RoundedCornerShape(20.dp))
                .border(2.dp, BlackColor, RoundedCornerShape(20.dp))
                .clickable { onPrivacyClick() }
                .testTag("privacy_navbar_button"),
            contentAlignment = Alignment.Center
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
                val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                drawRoundRect(
                    color = BlackColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, pathEffect = pathEffect),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                )
            }
            .clickable { onBrowseClick() }
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
