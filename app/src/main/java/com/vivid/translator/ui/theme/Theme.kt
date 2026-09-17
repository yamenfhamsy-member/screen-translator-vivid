package com.vivid.translator.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val VividColorScheme = darkColorScheme(
    primary = BoneWhite,
    onPrimary = Obsidian,
    background = Obsidian,
    onBackground = BoneWhite,
    surface = Obsidian,
    onSurface = BoneWhite,
    surfaceVariant = GraphiteVeil,
    onSurfaceVariant = BoneWhite,
    outline = AshBorder,
    secondary = FogBlue,
    onSecondary = Obsidian
)

private val VividShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(15.dp),
    extraLarge = RoundedCornerShape(15.dp)
)

@Composable
fun VividTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VividColorScheme,
        typography = VividTypography,
        shapes = VividShapes,
        content = content
    )
}
