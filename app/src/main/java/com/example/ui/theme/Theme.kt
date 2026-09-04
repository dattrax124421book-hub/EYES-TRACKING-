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
    primary = EyeEmerald,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00381C),
    onPrimaryContainer = Color(0xFF7BFFB0),
    secondary = EyeCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF00364B),
    onSecondaryContainer = Color(0xFF8CEEFF),
    tertiary = AccentAmber,
    onTertiary = Color.Black,
    background = DeepDarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkBorder,
    error = AccentRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = EyeEmeraldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F8D1),
    onPrimaryContainer = Color(0xFF00210E),
    secondary = EyeCyanDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBBE9FF),
    onSecondaryContainer = Color(0xFF001F2C),
    tertiary = Color(0xFFE65100),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorder,
    error = AccentRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to futuristic dark cyber-optic theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
