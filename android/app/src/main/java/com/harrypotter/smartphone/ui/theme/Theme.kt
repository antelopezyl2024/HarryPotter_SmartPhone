package com.harrypotter.smartphone.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HPColorScheme = darkColorScheme(
    primary          = HPGold,
    onPrimary        = HPBackground,
    secondary        = HPMaroon,
    onSecondary      = HPParchment,
    background       = HPBackground,
    onBackground     = HPParchment,
    surface          = HPSurface,
    onSurface        = HPParchment,
    surfaceVariant   = HPSurfaceVar,
    onSurfaceVariant = HPSilver,
    error            = HPWrong,
    outline          = HPCardBorder
)

@Composable
fun HarryPotterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HPColorScheme,
        typography  = HPTypography,
        content     = content
    )
}
