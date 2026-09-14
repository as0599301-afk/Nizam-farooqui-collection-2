package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NfcDarkColorScheme = darkColorScheme(
    primary = NfcGold,
    onPrimary = Color.Black,
    primaryContainer = NfcGoldDark,
    onPrimaryContainer = Color.White,
    secondary = NfcCyan,
    onSecondary = Color.Black,
    secondaryContainer = NfcBlue,
    onSecondaryContainer = Color.White,
    tertiary = NfcGreen,
    onTertiary = Color.Black,
    background = NfcMidnight,
    onBackground = TextPrimary,
    surface = NfcSurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = NfcSurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = NfcBorder
)

private val NfcLightColorScheme = lightColorScheme(
    primary = NfcBlue,
    onPrimary = Color.White,
    primaryContainer = NfcCyan,
    onPrimaryContainer = Color.Black,
    secondary = NfcGoldDark,
    onSecondary = Color.White,
    secondaryContainer = NfcGold,
    onSecondaryContainer = Color.Black,
    tertiary = NfcGreen,
    onTertiary = Color.White,
    background = Color(0xFFF4F6FB),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE8EDF5),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to rich audio dark theme for immersive music experience
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NfcDarkColorScheme else NfcLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
