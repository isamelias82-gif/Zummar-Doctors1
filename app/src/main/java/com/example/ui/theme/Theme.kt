package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimaryLight,
    onPrimary = Color.Black,
    primaryContainer = TealPrimary,
    onPrimaryContainer = Color.White,
    secondary = MintCyan,
    onSecondary = Color.Black,
    tertiary = CoralAccent,
    background = MedicalBackgroundDark,
    surface = MedicalSurfaceDark,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9)
)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = SoftCyanBg,
    onPrimaryContainer = TealPrimaryDark,
    secondary = TealSecondary,
    onSecondary = Color.White,
    secondaryContainer = SoftCyanBg,
    onSecondaryContainer = TealPrimaryDark,
    tertiary = CoralAccent,
    background = MedicalBackgroundLight,
    surface = MedicalSurfaceLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondary,
    outline = MedicalCardBorder,
    outlineVariant = Color(0xFFF1F5F9)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Enforce Light Mode as default theme
    dynamicColor: Boolean = false, // Keep clean medical web preview styling consistent
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

