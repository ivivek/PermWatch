package com.linetra.permalerts.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class HoloPalette(
    val bg: Color,
    val bgDeep: Color,
    val surface: Color,
    val surfaceHi: Color,
    val stroke: Color,
    val strokeHi: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkMute: Color,
    val accentA: Color,
    val accentB: Color,
    val accentC: Color,
    val warn: Color,
    val isDark: Boolean,
)

val HoloDark = HoloPalette(
    bg        = Color(0xFF04041B),
    bgDeep    = Color(0xFF01010E),
    surface   = Color(0xFFFFFFFF).copy(alpha = 0.05f),
    surfaceHi = Color(0xFFFFFFFF).copy(alpha = 0.14f),
    stroke    = Color(0xFFFFFFFF).copy(alpha = 0.14f),
    strokeHi  = Color(0xFFFFFFFF).copy(alpha = 0.22f),
    ink       = Color(0xFFECF2FF),
    inkSoft   = Color(0xFFB5C5DF),
    inkMute   = Color(0xFF7A90B4),
    accentA   = Color(0xFF00CDAA),
    accentB   = Color(0xFFF960CF),
    accentC   = Color(0xFF8F8CFF),
    warn      = Color(0xFFFF8600),
    isDark    = true,
)

val HoloLight = HoloPalette(
    bg        = Color(0xFFF7F8FC),
    bgDeep    = Color(0xFFE9EBF2),
    surface   = Color(0xFFFFFFFF).copy(alpha = 0.70f),
    surfaceHi = Color(0xFFFFFFFF).copy(alpha = 0.95f),
    stroke    = Color(0xFF12161F).copy(alpha = 0.10f),
    strokeHi  = Color(0xFF526EE3).copy(alpha = 0.35f),
    ink       = Color(0xFF12161F),
    inkSoft   = Color(0xFF454D5E),
    inkMute   = Color(0xFF768193),
    accentA   = Color(0xFF526EE3),
    accentB   = Color(0xFFBB4CB5),
    accentC   = Color(0xFF00A2AE),
    warn      = Color(0xFFE26C00),
    isDark    = false,
)

val LocalHolo = staticCompositionLocalOf<HoloPalette> {
    error("HoloPalette not provided — wrap content in PermWatchTheme")
}
