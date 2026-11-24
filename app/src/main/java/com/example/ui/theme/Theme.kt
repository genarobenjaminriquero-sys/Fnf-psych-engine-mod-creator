package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val StudioColorScheme = darkColorScheme(
    primary = StudioCyan,
    onPrimary = ObsidianBg,
    primaryContainer = StudioSurfaceElevated,
    onPrimaryContainer = StudioCyan,
    secondary = StudioIndigo,
    onSecondary = TextPrimary,
    secondaryContainer = StudioSurfaceHigh,
    onSecondaryContainer = TextPrimary,
    tertiary = LaneLeftColor,
    onTertiary = TextPrimary,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = HairlineBorder,
    outlineVariant = StudioSurfaceHigh,
    error = ErrorRed,
    onError = TextPrimary
)

val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun FunkinCraftTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioColorScheme,
        typography = FunkinTypography,
        shapes = StudioShapes,
        content = content
    )
}
