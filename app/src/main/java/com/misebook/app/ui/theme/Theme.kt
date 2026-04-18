package com.misebook.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightScheme = lightColorScheme(
    primary = Copper700,
    onPrimary = Sand50,
    primaryContainer = Copper300,
    onPrimaryContainer = Sand900,
    secondary = Sage700,
    onSecondary = Sand50,
    secondaryContainer = Sage300,
    onSecondaryContainer = Sand900,
    tertiary = Berry500,
    onTertiary = Sand50,
    background = Sand50,
    onBackground = Sand900,
    surface = Sand100,
    onSurface = Sand900,
    surfaceVariant = Sand200,
    onSurfaceVariant = Sand700,
    outline = Sand400,
    outlineVariant = Sand200,
    error = Berry500,
    onError = Sand50
)

private val DarkScheme = darkColorScheme(
    primary = Copper300,
    onPrimary = Sand900,
    primaryContainer = Copper700,
    onPrimaryContainer = Sand100,
    secondary = Sage300,
    onSecondary = Sand900,
    secondaryContainer = Sage700,
    onSecondaryContainer = Sand100,
    tertiary = Copper300,
    onTertiary = Sand900,
    background = Sand900,
    onBackground = Sand50,
    surface = Sand800,
    onSurface = Sand50,
    surfaceVariant = Sand700,
    onSurfaceVariant = Sand200,
    outline = Sand500,
    outlineVariant = Sand700,
    error = Berry500,
    onError = Sand900
)

@Composable
fun MiseBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MiseBookTypography,
        content = content
    )
}
