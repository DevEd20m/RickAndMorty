package com.example.rickandmorty.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RickAndMortyColorScheme = darkColorScheme(
    primary              = PortalTeal,
    onPrimary            = DeepSpace,
    primaryContainer     = PortalTealDim,
    onPrimaryContainer   = PortalTeal,
    secondary            = MutedBlue,
    onSecondary          = DeepSpace,
    secondaryContainer   = SpaceSurfaceAlt,
    onSecondaryContainer = StarWhite,
    tertiary             = UnknownGrey,
    onTertiary           = DeepSpace,
    background           = DeepSpace,
    onBackground         = StarWhite,
    surface              = SpaceSurface,
    onSurface            = StarWhite,
    surfaceVariant       = SpaceSurfaceAlt,
    onSurfaceVariant     = MutedBlue,
    outline              = CardBorder,
    error                = DeadRed,
    onError              = StarWhite,
)

@Composable
fun RickAndMortyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RickAndMortyColorScheme,
        typography  = Typography,
        content     = content
    )
}
