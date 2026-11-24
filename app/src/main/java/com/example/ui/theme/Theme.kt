package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val PulseDarkColorScheme = darkColorScheme(
    primary = PulseCrimson,
    onPrimary = PulseTextPrimary,
    primaryContainer = PulseCrimsonDark,
    onPrimaryContainer = PulseTextPrimary,
    secondary = PulseCyan,
    onSecondary = PulseObsidian,
    secondaryContainer = PulseCyanDim,
    onSecondaryContainer = PulseTextPrimary,
    tertiary = PulseAmber,
    onTertiary = PulseObsidian,
    background = PulseObsidian,
    onBackground = PulseTextPrimary,
    surface = PulseSurface,
    onSurface = PulseTextPrimary,
    surfaceVariant = PulseSurfaceElevated,
    onSurfaceVariant = PulseTextSecondary,
    surfaceContainer = PulseSurfaceElevated,
    surfaceContainerHigh = PulseSurfaceCard,
    outline = PulseOutline,
    error = PulseCrimson,
    onError = PulseTextPrimary
)

@Composable
fun ReelPulseTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(LocalContext.current)
    } else {
        PulseDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ReelPulseTypography,
        content = content
    )
}
