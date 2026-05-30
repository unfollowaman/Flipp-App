package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val FlippColorScheme = lightColorScheme(
    primary = BlackColor,
    onPrimary = WhiteColor,
    secondary = YellowColor,
    onSecondary = BlackColor,
    tertiary = PinkColor,
    onTertiary = BlackColor,
    background = BackgroundColor,
    onBackground = BlackColor,
    surface = CreamColor,
    onSurface = BlackColor,
    error = RedColor,
    onError = WhiteColor
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FlippColorScheme,
        typography = Typography,
        content = content
    )
}
