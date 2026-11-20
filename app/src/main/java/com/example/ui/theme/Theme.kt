package com.example.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle

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
        typography = Typography
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides TextStyle(fontFamily = BricolageGrotesque),
            content = content
        )
    }
}
