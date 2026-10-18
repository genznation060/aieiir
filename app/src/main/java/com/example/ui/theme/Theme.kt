package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MedicalDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = MedicalCyan,
    onSecondary = Color.White,
    secondaryContainer = MedicalCyanContainer,
    onSecondaryContainer = Color(0xFFCFFAFE),
    background = MedicalBackground,
    onBackground = TextPrimary,
    surface = MedicalSurface,
    onSurface = TextPrimary,
    surfaceVariant = MedicalSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    outlineVariant = MedicalSurfaceHighlight,
    error = MedicalRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = MedicalDarkColorScheme,
        typography = Typography,
        content = content
    )
}
