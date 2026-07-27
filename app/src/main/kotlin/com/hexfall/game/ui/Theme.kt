package com.hexfall.game.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object HexfallColors {
    val background = Color(0xFF120D1C)
    val surface = Color(0xFF1E1630)
    val surfaceLight = Color(0xFF2A2044)
    val gold = Color(0xFFE0B85C)
    val purple = Color(0xFF9B6DFF)
    val parchment = Color(0xFFEADFC8)
    val hpRed = Color(0xFFD9524A)
    val blockBlue = Color(0xFF5C9DE0)
    val energyAmber = Color(0xFFF2A93B)
    val attackRed = Color(0xFFB3543F)
    val skillBlue = Color(0xFF3F6FB3)
    val powerViolet = Color(0xFF7C4FB3)
    val curseGrey = Color(0xFF4A4A55)
    val poisonGreen = Color(0xFF6FAE4E)
}

private val DarkScheme = darkColorScheme(
    primary = HexfallColors.purple,
    secondary = HexfallColors.gold,
    background = HexfallColors.background,
    surface = HexfallColors.surface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = HexfallColors.parchment,
    onSurface = HexfallColors.parchment,
)

@Composable
fun HexfallTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
