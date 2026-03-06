package com.aurachat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * AuraChat always uses dark mode only (v1 spec).
 * True black (#000000) background for AMOLED battery efficiency.
 * Dynamic color is intentionally disabled — we enforce the Aura palette.
 */
private val AuraDarkColorScheme = darkColorScheme(
    primary              = AuraPrimary,
    onPrimary            = AuraOnPrimary,
    primaryContainer     = AuraPrimaryContainer,
    secondary            = AuraSecondary,
    onSecondary          = AuraOnSecondary,
    tertiary             = AuraTertiary,
    onTertiary           = AuraOnTertiary,
    background           = AuraBackground,
    onBackground         = AuraOnBackground,
    surface              = AuraSurface,
    onSurface            = AuraOnSurface,
    surfaceVariant       = AuraSurfaceVariant,
    onSurfaceVariant     = AuraOnSurfaceVariant,
    outline              = AuraOutline,
    outlineVariant       = AuraOutlineVariant,
    error                = AuraError,
    onError              = AuraOnError,
)

@Composable
fun AuraChatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AuraDarkColorScheme,
        typography  = Typography,
        content     = content
    )
}
