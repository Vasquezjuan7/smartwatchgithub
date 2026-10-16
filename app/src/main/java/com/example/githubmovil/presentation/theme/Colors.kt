package com.example.githubmovil.presentation.theme

import androidx.compose.ui.graphics.Color

/** Paleta "Aurora": fondo OLED negro + acentos neón inspirados en GitHub. */
object AppColors {
    val Background = Color(0xFF03050A)

    val Blue = Color(0xFF58A6FF)
    val Cyan = Color(0xFF39D0FF)
    val Purple = Color(0xFFA371F7)
    val Pink = Color(0xFFF778BA)
    val Green = Color(0xFF3FB950)
    val Teal = Color(0xFF2EE6A6)
    val Red = Color(0xFFFF5C5C)
    val Orange = Color(0xFFFF9E64)

    val TextMain = Color(0xFFE6EDF3)
    val TextMuted = Color(0xFF8B949E)

    val Glass = Color(0x14FFFFFF)
    val GlassStrong = Color(0x24FFFFFF)

    val Brand = listOf(Blue, Purple, Pink)
    val Success = listOf(Green, Teal)
    val Danger = listOf(Red, Orange)
}
