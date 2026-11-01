package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DaylightSlateColorScheme = lightColorScheme(
    primary = SlatePrimary,
    onPrimary = SlateOnPrimary,
    primaryContainer = SlatePrimaryContainer,
    onPrimaryContainer = SlateOnPrimaryContainer,
    secondary = CobaltAccent,
    onSecondary = CobaltOnAccent,
    secondaryContainer = CobaltContainer,
    onSecondaryContainer = CobaltOnContainer,
    tertiary = EmeraldCall,
    onTertiary = SlateOnPrimary,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = EmeraldOnContainer,
    error = CrimsonBlock,
    onError = SlateOnPrimary,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonOnContainer,
    background = DaylightBackground,
    onBackground = TextPrimaryLight,
    surface = DaylightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DaylightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = DaylightOutline,
    outlineVariant = DaylightOutlineStrong
)

private val DarkSlateColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkCobaltAccent,
    onSecondary = DarkBackground,
    secondaryContainer = DarkCobaltContainer,
    onSecondaryContainer = DarkCobaltOnContainer,
    tertiary = EmeraldCall,
    onTertiary = SlateOnPrimary,
    tertiaryContainer = EmeraldOnContainer,
    onTertiaryContainer = EmeraldContainer,
    error = CrimsonBlock,
    onError = SlateOnPrimary,
    errorContainer = CrimsonOnContainer,
    onErrorContainer = CrimsonContainer,
    background = DarkBackground,
    onBackground = DarkPrimary,
    surface = DarkSurface,
    onSurface = DarkPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DaylightOutlineStrong,
    outline = DarkOutline,
    outlineVariant = DarkOutline
)

val ClarityShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ClarityDialTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkSlateColorScheme else DaylightSlateColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ClarityShapes,
        content = content
    )
}
