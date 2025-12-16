package com.example.mynewsmobileappfe.core.ui.theme

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
    primary = SamsungBlueLightDark,
    onPrimary = Color.White,
    primaryContainer = SamsungBlueDark,
    onPrimaryContainer = SamsungBlueLightDark,

    secondary = SamsungSkyBlueDark,
    onSecondary = Color.White,
    secondaryContainer = SamsungNavy,
    onSecondaryContainer = SamsungSkyBlueDark,

    tertiary = SamsungSkyBlue,
    onTertiary = Color.White,

    background = SamsungBackgroundDark,
    onBackground = SamsungTextPrimaryDark,

    surface = SamsungSurfaceDark,
    onSurface = SamsungTextPrimaryDark,
    surfaceVariant = SamsungSurfaceVariantDark,
    onSurfaceVariant = SamsungTextSecondaryDark,

    error = SamsungError,
    onError = Color.White,

    outline = SamsungTextTertiary,
    outlineVariant = SamsungSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = SamsungBlue,
    onPrimary = Color.White,
    primaryContainer = SamsungBlueLight,
    onPrimaryContainer = SamsungNavy,

    secondary = SamsungSkyBlue,
    onSecondary = Color.White,
    secondaryContainer = SamsungBackgroundLight,
    onSecondaryContainer = SamsungBlueDark,

    tertiary = SamsungBlueDark,
    onTertiary = Color.White,

    background = SamsungBackgroundLight,
    onBackground = SamsungTextPrimary,

    surface = SamsungSurfaceLight,
    onSurface = SamsungTextPrimary,
    surfaceVariant = SamsungSurfaceVariantLight,
    onSurfaceVariant = SamsungTextSecondary,

    error = SamsungError,
    onError = Color.White,

    outline = SamsungTextTertiary,
    outlineVariant = SamsungSurfaceVariantLight
)

@Composable
fun MyNewsMobileAppFETheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
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