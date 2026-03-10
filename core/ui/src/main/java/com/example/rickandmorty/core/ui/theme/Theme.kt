package com.example.rickandmorty.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
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

private val LightColorScheme = lightColorScheme(
    primary              = PortalTeal,
    onPrimary            = LightOnPrimary,
    primaryContainer     = LightPrimaryContainer,
    onPrimaryContainer   = LightOnPrimaryContainer,
    secondary            = LightSecondary,
    onSecondary          = LightOnSecondary,
    secondaryContainer   = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary             = LightTertiary,
    onTertiary           = LightOnTertiary,
    background           = LightBackground,
    onBackground         = LightOnBackground,
    surface              = LightSurface,
    onSurface            = LightOnSurface,
    surfaceVariant       = LightSurfaceVariant,
    onSurfaceVariant     = LightOnSurfaceVariant,
    outline              = LightOutline,
    error                = DeadRed,
    onError              = StarWhite,
)

@Composable
fun RickAndMortyTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = Typography,
        content     = content
    )
}
