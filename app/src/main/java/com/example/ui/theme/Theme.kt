package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val EkisiityLightColors = lightColorScheme(
    primary = Sapphire900,
    onPrimary = Color.White,
    primaryContainer = Sapphire100,
    onPrimaryContainer = Sapphire950,
    secondary = Gold600,
    onSecondary = Color.White,
    secondaryContainer = Gold100,
    onSecondaryContainer = Ink900,
    tertiary = Emerald600,
    onTertiary = Color.White,
    tertiaryContainer = Emerald100,
    onTertiaryContainer = Ink900,
    error = Crimson600,
    onError = Color.White,
    errorContainer = Crimson100,
    onErrorContainer = Crimson600,
    background = IvoryBackground,
    onBackground = Ink900,
    surface = AlabasterSurface,
    onSurface = Ink900,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = Ink600,
    outline = OutlineLight,
)

private val EkisiityDarkColors = darkColorScheme(
    primary = Color(0xFF7CA9FF),
    onPrimary = Sapphire950,
    primaryContainer = Sapphire800,
    onPrimaryContainer = Sapphire100,
    secondary = Gold500,
    onSecondary = Sapphire950,
    secondaryContainer = Color(0xFF4A3700),
    onSecondaryContainer = Gold200,
    tertiary = Emerald500,
    onTertiary = Sapphire950,
    tertiaryContainer = Color(0xFF073D28),
    onTertiaryContainer = Emerald100,
    error = Color(0xFFFF8993),
    onError = Color(0xFF560711),
    errorContainer = Color(0xFF7D111E),
    onErrorContainer = Crimson100,
    background = NightBackground,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightInkMuted,
    outline = OutlineDark,
)

val EkisiityShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun EkisiityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> EkisiityDarkColors
        else -> EkisiityLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EkisiityTypography,
        shapes = EkisiityShapes,
        content = content,
    )
}
