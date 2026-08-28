package com.mystore.installments.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    primaryContainer = PrimaryBlueDark,
    secondary = AccentGreen,
    background = BackgroundLight,
    surface = SurfaceLight,
    error = AccentRed
)

@Composable
fun InstallmentSalesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content
    )
}
